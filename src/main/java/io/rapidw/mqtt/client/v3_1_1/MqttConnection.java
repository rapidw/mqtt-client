/*
 * Copyright 2020 Rapidw
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.rapidw.mqtt.client.v3_1_1;


import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.ssl.SslHandshakeCompletionEvent;
import io.netty.handler.timeout.*;
import io.netty.util.collection.IntObjectHashMap;
import io.netty.util.concurrent.Promise;
import io.netty.util.concurrent.ScheduledFuture;
import io.rapidw.mqtt.client.v3_1_1.handler.*;
import io.rapidw.mqtt.codec.v3_1_1.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class MqttConnection {
    private static Logger log = LoggerFactory.getLogger(MqttConnection.class);

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
    private LinkedHashMap<Integer, MqttPendingMessage> pendingMessages = new LinkedHashMap<>();
    private MqttTopicTree subscriptionTree = new MqttTopicTree();
    private Handler handler;
    private MqttConnectResultHandler mqttConnectResultHandler;
    private AtomicInteger currentPacketId = new AtomicInteger();
    private Promise<Void> closePromise;

    MqttConnection(Bootstrap bootstrap, MqttConnectionOption connectionOption) {
        this.connectionOption = connectionOption;
        this.bootstrap = bootstrap;
    }

    Handler handler() {
        handler = new Handler();
        return handler;
    }

    /**
     * establish connection
     * @param tcpConnectResultHandler handler for result of TCP connecting
     * @param mqttConnectResultHandler handler for result of MQTT connecting
     */
    public void connect(TcpConnectResultHandler tcpConnectResultHandler, MqttConnectResultHandler mqttConnectResultHandler) {
        this.closePromise = this.bootstrap.config().group().next().newPromise();
        if (status != Status.NOT_CONNECTING) {
            mqttConnectResultHandler.onError(new MqttClientException("invalid connection status: " + status.name()));
        }
        this.mqttConnectResultHandler = mqttConnectResultHandler;
        handler.connect(tcpConnectResultHandler);
    }

    /**
     * subscribe new topic
     * @param topicAndQosLevels topic and QoS level to subscribe
     * @param mqttMessageHandler handler for new messages from subscribed topic
     * @param subscribeResultHandler handler for subscription result
     */
    public void subscribe(List<MqttV311TopicAndQosLevel> topicAndQosLevels, MqttMessageHandler mqttMessageHandler, MqttSubscribeResultHandler subscribeResultHandler) {
        if (status != Status.CONNECTED) {
            subscribeResultHandler.onError(new MqttClientException("invalid connection status: " + status.name()));
        }
        handler.subscribe(topicAndQosLevels, mqttMessageHandler, subscribeResultHandler);
    }

    /**
     * publish message at QoS 0
     * @param topic topic
     * @param retain retain
     * @param payload payload
     */
    public void publishQos0Message(String topic, boolean retain, byte[] payload) {
        publish(topic, MqttV311QosLevel.AT_MOST_ONCE, retain, payload, null);
    }

    /**
     * publish message at QoS 0
     * @param topic topic
     * @param retain retain
     * @param payload payload
     * @param publishResultHandler handler for publish result
     */
    public void publishQos0Message(String topic, boolean retain, byte[] payload, MqttPublishResultHandler publishResultHandler) {
        publish(topic, MqttV311QosLevel.AT_MOST_ONCE, retain, payload, Objects.requireNonNull(publishResultHandler));
    }

    /**
     * publish message at QoS 1
     * @param topic topic
     * @param retain retain
     * @param payload payload
     * @param publishResultHandler handler for publish result
     */
    public void publishQos1Message(String topic, boolean retain, byte[] payload, MqttPublishResultHandler publishResultHandler) {
        publish(topic, MqttV311QosLevel.AT_LEAST_ONCE, retain, payload, Objects.requireNonNull(publishResultHandler));
    }

    /**
     * publish message at QoS 2, NOT supported now.
     * @param topic topic
     * @param retain retain
     * @param payload payload
     * @param publishResultHandler handler for publish result
     */
    public void publishQos2Message(String topic, boolean retain, byte[] payload, MqttPublishResultHandler publishResultHandler) {
        publish(topic, MqttV311QosLevel.EXACTLY_ONCE, retain, payload, Objects.requireNonNull(publishResultHandler));
    }

    private void publish(String topic, MqttV311QosLevel qos, boolean retain, byte[] payload, MqttPublishResultHandler publishResultHandler) {
        if (status != Status.CONNECTED) {
            publishResultHandler.onError(new MqttClientException("invalid connection status: " + status.name()));
        }
        handler.publish(topic, qos, retain, payload, publishResultHandler);
    }

    /**
     * unsubscribe a list of subscriptions. No more message will be received if unsubscribe success.Notice: this method is not implemented
     * by iteratively calling {@link #unsubscribe(MqttSubscription, MqttUnsubscribeResultHandler)}
     * @param subscriptions a list of MQTT subscriptions. You can get one item from {@link MqttSubscribeResultHandler}.
     * @param unsubscribeResultHandler handler for unsubscribe result
     */
    public void unsubscribe(List<MqttSubscription> subscriptions, MqttUnsubscribeResultHandler unsubscribeResultHandler) {
        handler.unsubscribe(subscriptions, unsubscribeResultHandler);
    }

    /**
     * unsubscribe one subscription. No more message will be received if unsubscribe success.
     * @param subscription MQTT subscription. You can get it from {@link MqttSubscribeResultHandler}.
     * @param unsubscribeResultHandler handler for unsubscribe result
     */
    void unsubscribe(MqttSubscription subscription, MqttUnsubscribeResultHandler unsubscribeResultHandler) {
        handler.unsubscribe(Collections.singletonList(subscription), unsubscribeResultHandler);
    }

    /**
     * close this connection
     */
    public void close() {
        handler.close();
    }

    /**
     * block the current thread until connection closed
     */
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

    class Handler extends SimpleChannelInboundHandler<MqttV311Packet> {

        private ScheduledFuture<?> pingRespTimeoutFuture;

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, MqttV311Packet packet) {
            switch (packet.getType()) {
                case CONNACK:
                    handleConnAck((MqttV311ConnAckPacket) packet);
                    break;
                case SUBACK:
                    handleSubAck((MqttV311SubAckPacket) packet);
                    break;
                case PUBLISH:
                    handlePublish((MqttV311PublishPacket) packet);
                    break;
                case UNSUBACK:
                    handleUnSubAck((MqttV311UnsubAckPacket) packet);
                    break;
                case PUBACK:
                    handlePubAck((MqttV311PubAckPacket) packet);
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

        private void handlePubAck(MqttV311PubAckPacket packet) {
            int curr = currentPacketId.get();
            if (packet.getPacketId() != curr) {
                throwException(new MqttClientException("invalid SUBACK packetId, required: " + curr + ", got: " + packet.getPacketId()));
            }

            MqttPendingMessage pending = pendingMessages.remove(curr);
            if (pending != null) {
                pending.getPublishResultHandler().onSuccess();
            } else {
                throwException(new MqttClientException("invalid packetId in PUBACK packet, pending message not found"));
            }
        }

        private void handlePublish(MqttV311PublishPacket packet) {
            boolean hasHandler = false;
            Set<MqttMessageHandler> handlers = subscriptionTree.getHandlersByTopicName(packet.getTopic());
            if (handlers.size() == 0) {
                throwException(new MqttClientException("PUBLISH packet without message handler received, topic: " + packet.getTopic()));
            }
            for (MqttMessageHandler handler: handlers) {
                handler.onMessage(packet.getTopic(), packet.getQosLevel(), packet.isRetain(), packet.isDupFlag(), packet.getPacketId(), packet.getPayload());
            }
            if (packet.getQosLevel() == MqttV311QosLevel.AT_LEAST_ONCE) {
                pubAck(packet.getPacketId());
            }
        }

        private void handleSubAck(MqttV311SubAckPacket packet) {
            int curr = currentPacketId.get();
            if (packet.getPacketId() != curr) {
                throwException(new MqttClientException("invalid SUBACK packetId, required: " + curr + ", got: " + packet.getPacketId()));
            }

            MqttPendingSubscription pending = pendingSubscriptions.remove(curr);
            if (pending != null) {
                List<MqttV311TopicAndQosLevel> topicAndQosLevels = pending.getTopicAndQosLevels();
                MqttMessageHandler messageHandler = pending.getMessageHandler();
                List<MqttSubscription> subscriptionList = new LinkedList<>();
                List<MqttV311QosLevel> packetQosList = packet.getQosLevels();

                if (topicAndQosLevels.size() != packetQosList.size()) {
                    throwException(new MqttClientException("count of topics in SUBACK packet does not match SUBSCRIBE packet"));
                }

                Iterator<MqttV311QosLevel> packIter = packetQosList.iterator();
                Iterator<MqttV311TopicAndQosLevel> pendingIter = topicAndQosLevels.iterator();

                while (packIter.hasNext() && pendingIter.hasNext()) {
                    MqttV311QosLevel qos = packIter.next();
                    MqttV311TopicAndQosLevel topicAndQos = pendingIter.next();

                    subscriptionTree.addSubscription(new MqttV311TopicAndQosLevel(topicAndQos.getTopicFilter(), qos), messageHandler);
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

        private void handleConnAck(MqttV311ConnAckPacket packet) {
            log.debug("handle CONACK");
            channel.pipeline().remove(MqttClientConstants.MQTT_CONNECT_TIMER);
            if (packet.getConnectReturnCode() == MqttV311ConnectReturnCode.CONNECTION_ACCEPTED) {
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

        private void handleUnSubAck(MqttV311UnsubAckPacket packet) {
            log.debug("handle UNSUBACK");

            int curr = currentPacketId.get();
            if (packet.getPacketId() != curr) {
                throwException(new MqttClientException("invalid UNSUBACK packetId, required: " + curr + ", got: " + packet.getPacketId()));
            }

            MqttPendingUnsubscription pending = pendingUnsubscriptions.remove(curr);
            if (pending != null) {
                for (String topicFilter: pending.getTopicFilters()) {
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
            log.debug("channel active");
            if (connectionOption.getServerCertificate() == null) {
                log.debug("raw socket");
                doConnect(ctx);
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            log.debug("error", cause);
            if (cause instanceof ReadTimeoutException && status == Status.CONNECTING) {
                mqttConnectResultHandler.onTimeout();
            } else if (cause instanceof DecoderException) {
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
                    log.debug("send PINGREQ");
                    this.pingReq(ctx.channel());
                }
            }
            if (evt instanceof SslHandshakeCompletionEvent) {
                log.debug("ssl socket");
                doConnect(ctx);
            }
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) {
            closePromise.setSuccess(null);
            log.debug("connection closed");
        }

        private void doConnect(ChannelHandlerContext ctx) {
            log.debug("channel active");

            MqttV311ConnectPacket.MqttV311ConnectPacketBuilder packetBuilder = MqttV311ConnectPacket.builder()
                .username(connectionOption.getUsername())
                .password(connectionOption.getPassword())
                .clientId(connectionOption.getClientId())
                .keepaliveSeconds(connectionOption.getKeepAliveSeconds())
                .cleanSession(connectionOption.isCleanSession());

            if (connectionOption.getWill() != null) {
                packetBuilder.will(MqttV311Will.builder()
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

        //----------------------------------------------------------------------------------------------------

        public void connect(TcpConnectResultHandler tcpMqttConnectResultHandler) {
            bootstrap.connect(connectionOption.getHost(), connectionOption.getPort()).addListener(future -> {
                ChannelFuture channelFuture = (ChannelFuture) future;
                if (future.isSuccess()) {
                    channel = channelFuture.channel();
                    tcpMqttConnectResultHandler.onSuccess();
                } else {
                    Throwable cause = future.cause();
                    if (cause instanceof ConnectTimeoutException) {
                        tcpMqttConnectResultHandler.onTimeout();
                    } else {
                        tcpMqttConnectResultHandler.onError(cause);
                    }
                    status = Status.CLOSED;
                    closePromise.setSuccess(null);
                }
            });
            status = Status.CONNECTING;
        }

        public void subscribe(List<MqttV311TopicAndQosLevel> topicAndQosList, MqttMessageHandler mqttMessageHandler, MqttSubscribeResultHandler mqttSubscribeResultHandler) {
            int packetId = nextPacketId();

            MqttV311SubscribePacket packet = MqttV311SubscribePacket.builder()
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

        public void publish(String topic, MqttV311QosLevel qosLevel, boolean retain, byte[] payload, MqttPublishResultHandler mqttPublishResultHandler) {
            if (qosLevel == MqttV311QosLevel.EXACTLY_ONCE) {
                throw new UnsupportedOperationException("publish with qos1 or qos2 current unsupported");
            }
            int packetId = 0;
            MqttV311PublishPacket.MqttV311PublishPacketBuilder builder = MqttV311PublishPacket.builder()
                .topic(topic)
                .qosLevel(qosLevel)
                .dupFlag(false)
                .retain(retain)
                .payload(payload);
            if (qosLevel == MqttV311QosLevel.AT_LEAST_ONCE) {
                packetId = nextPacketId();
                builder.packetId(packetId);
            }

            int finalPacketId = packetId;
            MqttV311PublishPacket packet = builder.build();
            channel.writeAndFlush(packet).addListener(future -> {

                if (qosLevel == MqttV311QosLevel.AT_MOST_ONCE) {
                    if ( mqttPublishResultHandler != null) {
                        if (future.isSuccess()) {
                            mqttPublishResultHandler.onSuccess();
                        } else {
                            mqttPublishResultHandler.onError(future.cause());
                        }
                    }
                } else {
                    if (future.isSuccess()) {
                        pendingMessages.put(finalPacketId, MqttPendingMessage.builder()
                            .packet(packet)
                            .publishResultHandler(mqttPublishResultHandler)
                            .build());
                    } else {
                        mqttPublishResultHandler.onError(future.cause());
                    }
                }
            });
            switch (qosLevel) {
                case AT_MOST_ONCE:

                    break;
                case AT_LEAST_ONCE:

            }
        }

        private void pingReq(Channel channel) {
            if (this.pingRespTimeoutFuture == null) {
                this.pingRespTimeoutFuture = channel.eventLoop().schedule( () -> {
                    channel.writeAndFlush(MqttV311DisconnectPacket.INSTANCE).addListener(ChannelFutureListener.CLOSE);
                }, connectionOption.getKeepAliveSeconds(), TimeUnit.SECONDS);
            }
            channel.writeAndFlush(MqttV311PingReqPacket.INSTANCE)
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
            int packetId = nextPacketId();

            List<String> topicFilters = new LinkedList<>();
            subscriptions.forEach(v -> topicFilters.add(v.getTopicFilter()));

            MqttV311UnsubscribePacket packet = MqttV311UnsubscribePacket.builder()
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

        public void pubAck(int packetId) {
            MqttV311PubAckPacket packet = MqttV311PubAckPacket.builder().packetId(packetId).build();
            channel.writeAndFlush(packet).addListener( future -> {
                if (!future.isSuccess()) {
                    throwException(future.cause());
                }
            });
        }

        private void throwException(Throwable cause) {
            close();
            connectionOption.getExceptionHandler().onException(cause);
        }
    }
}
