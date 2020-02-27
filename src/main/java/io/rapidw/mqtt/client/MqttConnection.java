package io.rapidw.mqtt.client;


import io.rapidw.mqtt.client.handler.*;
import io.rapidw.mqtt.codec.*;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.timeout.*;
import io.netty.util.collection.IntObjectHashMap;
import io.netty.util.concurrent.Promise;
import io.netty.util.concurrent.ScheduledFuture;
import io.rapidw.mqtt.client.handler.*;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class MqttConnection {

    private enum Status {
        NOT_CONNECTING,
        CONNECTING,
        CONNECTED,
        CLOSED
    }

    private MqttConnectionOption connectionOption;
    private Bootstrap bootstrap;
    private Status status = Status.NOT_CONNECTING;
    private Channel channel;
    private IntObjectHashMap<MqttPendingSubscription> pendingSubscriptions = new IntObjectHashMap<>();
    private IntObjectHashMap<MqttPendingUnsubscription> pendingUnsubscriptions = new IntObjectHashMap<>();
    private MqttTopicTree subscriptionTree = new MqttTopicTree();
    private Handler handler;
    private MqttConnectResultHandler mqttConnectResultHandler;
    private AtomicInteger currentPacketId = new AtomicInteger();
    private Promise<Void> closePromise;

    public MqttConnection(Bootstrap bootstrap, MqttConnectionOption connectionOption) {
        this.connectionOption = connectionOption;
        this.bootstrap = bootstrap;
    }

    public Handler handler() {
        handler = new Handler();
        return handler;
    }

    public void connect(TcpConnectResultHandler tcpConnectResultHandler, MqttConnectResultHandler mqttConnectResultHandler) {
        this.closePromise = this.bootstrap.config().group().next().newPromise();
        if (status != Status.NOT_CONNECTING) {
            mqttConnectResultHandler.onError(new MqttClientException("invalid connection status: " + status.name()));
        }
        this.mqttConnectResultHandler = mqttConnectResultHandler;
        handler.connect(tcpConnectResultHandler);
    }

    public void subscribe(List<MqttTopicAndQosLevel> topicAndQosLevels, MqttMessageHandler mqttMessageHandler, MqttSubscribeResultHandler subscribeResultHandler) {
        if (status != Status.CONNECTED) {
            subscribeResultHandler.onError(new MqttClientException("invalid connection status: " + status.name()));
        }
        handler.subscribe(topicAndQosLevels, mqttMessageHandler, subscribeResultHandler);
    }

    public void publish(String topic, MqttQosLevel qos, boolean retain, byte[] payload, MqttPublishResultHandler publishResultHandler) {
        if (status != Status.CONNECTED) {
            publishResultHandler.onError(new MqttClientException("invalid connection status: " + status.name()));
        }
        handler.publish(topic, qos, retain, payload, publishResultHandler);
    }

    public void unsubscribe(List<MqttSubscription> subscriptions, MqttUnsubscribeResultHandler unsubscribeResultHandler) {
        handler.unsubscribe(subscriptions, unsubscribeResultHandler);
    }

    void unsubscribe(MqttSubscription subscription, MqttUnsubscribeResultHandler unsubscribeResultHandler) {
        handler.unsubscribe(Collections.singletonList(subscription), unsubscribeResultHandler);
    }

    public void close() {
        handler.close();
    }

    public void waitForClose() {
        try {
            closePromise.await();
        } catch (InterruptedException e) {
            connectionOption.getExceptionHandler().onException(e);
        }
    }

    // ------------------------------------------------------------------------------------------------

    private int nextPacketId() {
        return currentPacketId.accumulateAndGet(1, (current, update) -> (current += update) > 65535 ? 1 : current);
    }

    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    class Handler extends SimpleChannelInboundHandler<MqttPacket> {

        private ScheduledFuture<?> pingRespTimeoutFuture;

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, MqttPacket packet) {
            switch (packet.getType()) {
                case CONNACK:
                    handleConnAck((MqttConnAckPacket) packet);
                    break;
                case SUBACK:
                    handleSubAck((MqttSubAckPacket) packet);
                    break;
                case PUBLISH:
                    handlePublish((MqttPublishPacket) packet);
                    break;
                case UNSUBACK:
                    handleUnSubAck((MqttUnsubAckPacket) packet);
                    break;
                case PUBACK:
                    //                handlePubAck();
                    break;
                case PUBREC:
                    //                handlePubRec();
                    break;
                case PUBREL:
                    //                handlePubRel();
                    break;
                case PUBCOMP:
                    //                handlePubComp();
                    break;
                case PINGRESP:
                    handlePingResp();
                    break;
                default:
                    log.warn("unknown message Type");
            }
        }

        private void handlePublish(MqttPublishPacket packet) {
            boolean hasHandler = false;
            val handlers = subscriptionTree.getHandlersByTopicName(packet.getTopic());
            for (MqttMessageHandler handler: handlers) {
                hasHandler = true;
                handler.onMessage(packet.getTopic(), packet.getQosLevel(), packet.isRetain(), packet.isDupFlag(), packet.getPacketId(), packet.getPayload());
            }
            if (!hasHandler) {
                throwException(new MqttClientException("PUBLISH packet without message handler received, topic: " + packet.getTopic()));
            }
        }

        private void handleSubAck(MqttSubAckPacket packet) {
            val curr = currentPacketId.get();
            if (packet.getPacketId() != curr) {
                throwException(new MqttClientException("invalid SUBACK packetId, required: " + curr + ", got: " + packet.getPacketId()));
            }

            MqttPendingSubscription pending = pendingSubscriptions.get(packet.getPacketId());
            if (pending != null) {
                val topicAndQosLevels = pending.getTopicAndQosLevels();
                val messageHandler = pending.getMessageHandler();
                val subscriptionList = new LinkedList<MqttSubscription>();
                val packetQosList = packet.getQosLevels();

                if (topicAndQosLevels.size() != packetQosList.size()) {
                    throwException(new MqttClientException("count of topics in SUBACK packet does not match SUBSCRIBE packet"));
                }

                val packIter = packetQosList.iterator();
                val pendingIter = topicAndQosLevels.iterator();

                while (packIter.hasNext() && pendingIter.hasNext()) {
                    val qos = packIter.next();
                    val topicAndQos = pendingIter.next();

                    subscriptionTree.addSubscription(new MqttTopicAndQosLevel(topicAndQos.getTopicFilter(), qos), messageHandler);
                    subscriptionList.add(MqttSubscription.builder()
                        .connection(MqttConnection.this)
                        .topicFilter(topicAndQos.getTopicFilter())
                        .messageHandler(messageHandler)
                        .topicFilter(topicAndQos.getTopicFilter())
                        .qosLevel(qos)
                        .build()
                    );
                }

                pending.getMqttSubscribeResultHandler().onSuccess(subscriptionList);
            } else {
                throwException(new MqttClientException("invalid packetId in SUBACK packet, pending subscription not found"));
            }
        }

        private void handleConnAck(MqttConnAckPacket packet) {
            log.debug("handle CONACK");
            channel.pipeline().remove(MqttClientConstants.MQTT_CONNECT_TIMER);
            if (packet.getConnectReturnCode() == MqttConnectReturnCode.CONNECTION_ACCEPTED) {
                if (connectionOption.getKeepAliveSeconds() > 0) {
                    // 添加IdleStateHandler,当连接处于idle状态时间超过设定，会发送userEvent，触发心跳协议处理
                    channel.pipeline().addBefore(MqttClientConstants.MQTT_HANDLER, MqttClientConstants.KEEPALIVE_HANDLER,
                        new IdleStateHandler(0,
                            connectionOption.getKeepAliveSeconds() - connectionOption.getKeepAliveSecondsOffset(),
                            0));
                }
                status = Status.CONNECTED;
                mqttConnectResultHandler.onSuccess();
            } else {
                channel.close();
                mqttConnectResultHandler.onError(new MqttConnectionException(packet.getConnectReturnCode()));
            }
        }

        private void handlePingResp() {
            log.debug("handle PINGRESP");
            if (this.pingRespTimeoutFuture != null && !pingRespTimeoutFuture.isCancelled() && !this.pingRespTimeoutFuture.isDone()) {
                this.pingRespTimeoutFuture.cancel(true);
                this.pingRespTimeoutFuture = null;
            }
        }

        private void handleUnSubAck(MqttUnsubAckPacket packet) {
            log.debug("handle UNSUBACK");

            val curr = currentPacketId.get();
            if (packet.getPacketId() != curr) {
                throwException(new MqttClientException("invalid UNSUBACK packetId, required: " + curr + ", got: " + packet.getPacketId()));
            }

            MqttPendingUnsubscription pending = pendingUnsubscriptions.remove(packet.getPacketId());

            if (pending != null) {
                for (val topicFilter: pending.getTopicFilters()) {
                    subscriptionTree.removeSubscription(topicFilter);
                }
                pending.getUnsubscribeResultHandler().onSuccess();
            } else {
                throwException(new MqttClientException("invalid packetId in UNSUBACK packet, pending unsubscription not found"));
            }
        }

        // ------------------------------------------------------------------------------------------

        @Override
        public void channelActive(ChannelHandlerContext ctx) {

            val packetBuilder = MqttConnectPacket.builder()
                .username(connectionOption.getUsername())
                .password(connectionOption.getPassword())
                .clientId(connectionOption.getClientId())
                .keepaliveSeconds(connectionOption.getKeepAliveSeconds())
                .cleanSession(connectionOption.isCleanSession());

            if (connectionOption.getWill() != null) {
                packetBuilder.will(MqttWill.builder()
                    .topic(connectionOption.getWill().getTopic())
                    .message(connectionOption.getWill().getMessage())
                    .retain(connectionOption.getWill().isRetain())
                    .qosLevel(connectionOption.getWill().getQosLevel())
                    .build()
                );
            }

            ctx.channel().writeAndFlush(packetBuilder.build())
                .addListener(future -> {
                    if (future.isSuccess()) {
                        log.debug("send connect success");
                    } else {
                        log.debug("send connect failed", future.cause());
                        status = Status.CLOSED;
                        ctx.close();
                    }
                });
            if (connectionOption.getMqttConnectTimeout() > 0) {
                ctx.pipeline().addBefore(MqttClientConstants.MQTT_HANDLER, MqttClientConstants.MQTT_CONNECT_TIMER,
                    new ReadTimeoutHandler(connectionOption.getMqttConnectTimeout(), TimeUnit.MILLISECONDS));
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            log.debug("error", cause);
            if (cause instanceof ReadTimeoutException && status == Status.CONNECTING) {
                mqttConnectResultHandler.onTimeout();
            } else if (cause instanceof DecoderException) {
                log.error("decoder exception", cause);
                throwException(new MqttClientException("decoder exception", cause));
            } else {
                super.exceptionCaught(ctx, cause);
            }
        }


        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
            if (evt instanceof IdleStateEvent) {
                IdleStateEvent idleStateEvent = (IdleStateEvent) evt;
                if (idleStateEvent.state() == IdleState.WRITER_IDLE) {
                    log.debug("send pingreq");
                    this.pingReq(ctx.channel());
                }
            }
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) {
            closePromise.setSuccess(null);
            log.debug("connection closed");
        }

        //----------------------------------------------------------------------------------------------------

        public void connect(TcpConnectResultHandler tcpMqttConnectResultHandler) {
            bootstrap.connect(connectionOption.getHost(), connectionOption.getPort()).addListener(future -> {
                ChannelFuture channelFuture = (ChannelFuture) future;
                if (future.isSuccess()) {
                    channel = channelFuture.channel();
                    tcpMqttConnectResultHandler.onSuccess();
                } else {
                    if (future instanceof ConnectTimeoutException) {
                        tcpMqttConnectResultHandler.onTimeout();
                    } else {
                        tcpMqttConnectResultHandler.onError(channelFuture.cause());
                    }
                    status = Status.CLOSED;
                    closePromise.setSuccess(null);
                }
            });
            status = Status.CONNECTING;
        }

        public void subscribe(List<MqttTopicAndQosLevel> topicAndQosList, MqttMessageHandler mqttMessageHandler, MqttSubscribeResultHandler mqttSubscribeResultHandler) {
            int packetId = nextPacketId();

            MqttSubscribePacket packet = MqttSubscribePacket.builder()
                .packetId(packetId)
                .topicAndQosLevels(topicAndQosList)
                .build();

            channel.writeAndFlush(packet).addListener(future -> {
                if (future.isSuccess()) {
                    MqttPendingSubscription pendingSubscription = MqttPendingSubscription.builder()
                        .topicAndQosLevels(topicAndQosList)
                        .messageHandler(mqttMessageHandler)
                        .mqttSubscribeResultHandler(mqttSubscribeResultHandler)
                        .build();
                    pendingSubscriptions.put(packetId, pendingSubscription);
                } else {
                    mqttSubscribeResultHandler.onError(future.cause());
                }
            });
        }

        public void publish(String topic, MqttQosLevel qos, boolean retain, byte[] payload, MqttPublishResultHandler mqttPublishResultHandler) {
            if (qos == MqttQosLevel.AT_LEAST_ONCE || qos == MqttQosLevel.EXACTLY_ONCE) {
                throw new UnsupportedOperationException("publish with qos1 or qos2 current unsupported");
            }
            channel.writeAndFlush(MqttPublishPacket.builder()
                .topic(topic)
                .qosLevel(qos)
                .dupFlag(false)
                .retain(retain)
                .packetId(nextPacketId())
                .payload(payload)
                .build()
            ).addListener(future -> {
                if (future.isSuccess()) {
                    mqttPublishResultHandler.onSuccess();
                } else {
                    mqttPublishResultHandler.onError(future.cause());
                }
            });
        }

        private void pingReq(Channel channel) {
            if (this.pingRespTimeoutFuture == null) {
                this.pingRespTimeoutFuture = channel.eventLoop().schedule( () -> {
                    channel.writeAndFlush(MqttDisconnectPacket.INSTANCE).addListener(ChannelFutureListener.CLOSE);
                }, connectionOption.getKeepAliveSeconds(), TimeUnit.SECONDS);
            }
            channel.writeAndFlush(MqttPingReqPacket.INSTANTCE)
                .addListener(future -> {
                    if (!future.isSuccess()) {
                        throwException(new MqttClientException("send PINGREQ error", future.cause()));
                    }
                });
        }

        public void close() {
            status = Status.CLOSED;
            channel.close();
        }

        public void unsubscribe(List<MqttSubscription> subscriptions, MqttUnsubscribeResultHandler unsubscribeResultHandler) {
            val packetId = nextPacketId();

            val topicFilters = new LinkedList<String>();
            subscriptions.forEach(v -> topicFilters.add(v.getTopicFilter()));

            MqttUnsubscribePacket packet = MqttUnsubscribePacket.builder()
                .topicFilters(topicFilters)
                .packetId(packetId)
                .build();
            channel.writeAndFlush(packet).addListener(future -> {
                if (future.isSuccess()) {
                    pendingUnsubscriptions.put(packetId, new MqttPendingUnsubscription(topicFilters, unsubscribeResultHandler));
                } else {
                    unsubscribeResultHandler.onError(future.cause());
                }
            });
        }

        private void throwException(Throwable cause) {
            close();
            connectionOption.getExceptionHandler().onException(cause);
        }
    }
}
