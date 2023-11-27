/**
 * Copyright 2023 Rapidw
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
import io.netty.util.HashedWheelTimer;
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
    private final static Logger log = LoggerFactory.getLogger(MqttConnection.class);

    private enum Status {
        NOT_CONNECTING,
        CONNECTING,
        CONNECTED,
        CLOSED
    }

    private final MqttConnectionOption connectionOption;
    private final Bootstrap bootstrap;
    private Status status = Status.NOT_CONNECTING;
    private Channel channel;
    private final IntObjectHashMap<MqttPendingSubscription> pendingSubscriptions = new IntObjectHashMap<>();
    private final IntObjectHashMap<MqttPendingUnsubscription> pendingUnsubscribes = new IntObjectHashMap<>();
    private final LinkedHashMap<Integer, MqttQos1TxMessage> pendingTxQos1Messages = new LinkedHashMap<>();
    private final LinkedHashMap<Integer, MqttQos2TxMessage> pendingTxQos2Messages = new LinkedHashMap<>();
    private final LinkedHashMap<Integer, MqttQos2RxMessage> pendingRxQos2Messages = new LinkedHashMap<>();
    private final MqttTopicTree subscriptionTree = new MqttTopicTree();
    private Handler handler;
    private MqttConnectResultHandler mqttConnectResultHandler;
    private final AtomicInteger currentPacketId = new AtomicInteger();
    private Promise<Void> closePromise;
    private final HashedWheelTimer timer = new HashedWheelTimer(100, TimeUnit.MILLISECONDS, 100);

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
     *
     * @param tcpConnectResultHandler  handler for result of TCP connecting
     * @param mqttConnectResultHandler handler for result of MQTT connecting
     */
    public void connect(TcpConnectResultHandler tcpConnectResultHandler, MqttConnectResultHandler mqttConnectResultHandler) {
        this.closePromise = this.bootstrap.config().group().next().newPromise();
        if (status != Status.NOT_CONNECTING) {
            mqttConnectResultHandler.onError(this, new MqttClientException("invalid connection status: " + status.name()));
        }
        this.mqttConnectResultHandler = mqttConnectResultHandler;
        handler.connect(tcpConnectResultHandler);
    }

    /**
     * subscribe new topic
     *
     * @param topicAndQosLevels      topic and QoS level to subscribe
     * @param mqttMessageHandler     handler for new messages from subscribed topic
     * @param subscribeResultHandler handler for subscription result
     */
    public void subscribe(List<MqttV311TopicAndQosLevel> topicAndQosLevels, MqttMessageHandler mqttMessageHandler, MqttSubscribeResultHandler subscribeResultHandler) {
        if (status != Status.CONNECTED) {
            subscribeResultHandler.onError(this, new MqttClientException("invalid connection status: " + status.name()));
        }
        handler.subscribe(topicAndQosLevels, mqttMessageHandler, subscribeResultHandler);
    }

    /**
     * publish message at QoS 0
     *
     * @param topic   topic
     * @param retain  retain
     * @param payload payload
     */
    public void publishQos0Message(String topic, boolean retain, byte[] payload) {
        publish(topic, MqttV311QosLevel.AT_MOST_ONCE, retain, payload, null);
    }

    /**
     * publish message at QoS 0
     *
     * @param topic                topic
     * @param retain               retain
     * @param payload              payload
     * @param publishResultHandler handler for publish result
     */
    public void publishQos0Message(String topic, boolean retain, byte[] payload, MqttPublishResultHandler publishResultHandler) {
        publish(topic, MqttV311QosLevel.AT_MOST_ONCE, retain, payload, Objects.requireNonNull(publishResultHandler));
    }

    /**
     * publish message at QoS 1
     *
     * @param topic                topic
     * @param retain               retain
     * @param payload              payload
     * @param publishResultHandler handler for publish result
     */
    public void publishQos1Message(String topic, boolean retain, byte[] payload, MqttPublishResultHandler publishResultHandler) {
        publish(topic, MqttV311QosLevel.AT_LEAST_ONCE, retain, payload, Objects.requireNonNull(publishResultHandler));
    }

    /**
     * publish message at QoS 2, NOT supported now.
     *
     * @param topic                topic
     * @param retain               retain
     * @param payload              payload
     * @param publishResultHandler handler for publish result
     */
    public void publishQos2Message(String topic, boolean retain, byte[] payload, MqttPublishResultHandler publishResultHandler) {
        publish(topic, MqttV311QosLevel.EXACTLY_ONCE, retain, payload, Objects.requireNonNull(publishResultHandler));
    }

    private void publish(String topic, MqttV311QosLevel qos, boolean retain, byte[] payload, MqttPublishResultHandler publishResultHandler) {
        if (status != Status.CONNECTED) {
            publishResultHandler.onError(this, new MqttClientException("invalid connection status: " + status.name()));
        }
        handler.publish(topic, qos, retain, payload, publishResultHandler);
    }

    /**
     * unsubscribe a list of subscriptions. No more message will be received if unsubscribe success.Notice: this method is not implemented
     * by iteratively calling {@link #unsubscribe(MqttSubscription, MqttUnsubscribeResultHandler)}
     *
     * @param subscriptions            a list of MQTT subscriptions. You can get one item from {@link MqttSubscribeResultHandler}.
     * @param unsubscribeResultHandler handler for unsubscribe result
     */
    public void unsubscribe(List<MqttSubscription> subscriptions, MqttUnsubscribeResultHandler unsubscribeResultHandler) {
        handler.unsubscribe(subscriptions, unsubscribeResultHandler);
    }

    /**
     * unsubscribe one subscription. No more message will be received if unsubscribe success.
     *
     * @param subscription             MQTT subscription. You can get it from {@link MqttSubscribeResultHandler}.
     * @param unsubscribeResultHandler handler for unsubscribe result
     */
    void unsubscribe(MqttSubscription subscription, MqttUnsubscribeResultHandler unsubscribeResultHandler) {
        handler.unsubscribe(Collections.singletonList(subscription), unsubscribeResultHandler);
    }

    /**
     * disconnect this connection
     */
    public void disconnect() {
        handler.disconnect();
    }

    /**
     * block the current thread until connection closed
     */
    public void waitForClose() {
        try {
            closePromise.await();
        } catch (InterruptedException e) {
            connectionOption.getExceptionHandler().onException(this, e);
        }
    }

    // ------------------------------------------------------------------------------------------------

    private int nextPacketId() {
        return currentPacketId.accumulateAndGet(1, (current, update) -> {
            int next = findNext(current, update);
            while (pendingTxQos1Messages.containsKey(next) || pendingTxQos2Messages.containsKey(next)) {
                next = findNext(next, update);
            }
            return next;
        });
    }

    private int findNext(int current, int update) {
        int next = current + update;
        if (next > 65535) {
            next = 1;
        }
        return next;
    }

    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    class Handler extends SimpleChannelInboundHandler<MqttV311Packet> {

        private ScheduledFuture<?> pingRespTimeoutFuture;

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, MqttV311Packet packet) {
            log.debug("receive packet: {}", packet.getType());
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
                    handlePubRec((MqttV311PubRecPacket) packet);
                    break;
                case PUBREL:
                    handlePubRel((MqttV311PubRelPacket) packet);
                    break;
                case PUBCOMP:
                    handlePubComp((MqttV311PubCompPacket) packet);
                    break;
                case PINGRESP:
                    handlePingResp();
                    break;
                default:
                    log.warn("unknown message Type");
            }
        }

        // qos 1 tx stage 1
        private void handlePubAck(MqttV311PubAckPacket packet) {
            MqttQos1TxMessage pending = pendingTxQos1Messages.remove(packet.getPacketId());
            if (pending != null) {
                pending.getTimeout().cancel();
                pending.getPublishResultHandler().onSuccess(MqttConnection.this);
            } else {
                throwException(new MqttClientException("invalid packetId in PUBACK packet, pending publish message not found"));
            }
        }

        // qos 2 tx stage 1
        private void handlePubRec(MqttV311PubRecPacket packet) {
            log.debug("handle PUBREC {}", packet.getPacketId());
            MqttQos2TxMessage pending = pendingTxQos2Messages.get(packet.getPacketId());
            if (pending != null) {
                if (pending.getStatus() == MqttQos2TxMessage.Status.PUBLISH) {
                    pending.getTimeout().cancel();
                    pubRel(packet.getPacketId());
                    pending.setStatus(MqttQos2TxMessage.Status.PUBREL);
                    pending.setTimeout(timer.newTimeout(timeout -> throwException(new MqttClientException("PUBCOMP timeout")),
                        connectionOption.getPubCompTimeout(), TimeUnit.SECONDS));
                } else {
                    throwException(new MqttClientException("invalid status " + pending.getStatus() + " in qos2 pending publish message"));
                }
            } else {
                throwException(new MqttClientException("invalid packetId in PUBREC packet, pending publish message not found"));
            }
        }

        // qos 2 rx stage 2
        private void handlePubRel(MqttV311PubRelPacket packet) {
            MqttQos2RxMessage pending = pendingRxQos2Messages.remove(packet.getPacketId());
            if (pending != null) {
                pending.getTimeout().cancel();
                pubComp(packet.getPacketId());
                handleSubscriptions(pending.getPacket());
            } else {
                throwException(new MqttClientException("invalid packetId in PUBREL packet, pending receive message not found"));
            }
        }

        // qos 2 tx stage 2
        private void handlePubComp(MqttV311PubCompPacket packet) {
            MqttQos2TxMessage pending = pendingTxQos2Messages.remove(packet.getPacketId());
            if (pending != null && pending.getStatus() == MqttQos2TxMessage.Status.PUBREL) {
                pending.getTimeout().cancel();
                pending.getPublishResultHandler().onSuccess(MqttConnection.this);
            } else {
                throwException(new MqttClientException("invalid packetId in PUBACK packet, pending message not found"));
            }
        }

        private void handlePublish(MqttV311PublishPacket packet) {
            switch (packet.getQosLevel()) {
                case AT_MOST_ONCE:
                    handlePublishQos0(packet);
                    break;
                case AT_LEAST_ONCE:
                    handlePublishQos1(packet);
                    break;
                case EXACTLY_ONCE:
                    handlePublishQos2(packet);
                    break;
            }
        }

        private void handleSubscriptions(MqttV311PublishPacket packet) {
            Set<MqttMessageHandler> handlers = subscriptionTree.getHandlersByTopicName(packet.getTopic());
            if (handlers.isEmpty()) {
                throwException(new MqttClientException("PUBLISH packet without message handler received, topic: " + packet.getTopic()));
            }
            for (MqttMessageHandler handler : handlers) {
                handler.onMessage(MqttConnection.this, packet.getTopic(), packet.getQosLevel(),
                    packet.isRetain(), packet.isDupFlag(), packet.getPacketId(), packet.getPayload());
            }
        }

        private void handlePublishQos0(MqttV311PublishPacket packet) {
            handleSubscriptions(packet);
        }

        // qos 1 rx stage 1
        private void handlePublishQos1(MqttV311PublishPacket packet) {
            pubAck(packet.getPacketId());
            handleSubscriptions(packet);
        }

        // qos 2 rx stage 1
        public void handlePublishQos2(MqttV311PublishPacket packet) {
            MqttQos2RxMessage pending = pendingRxQos2Messages.get(packet.getPacketId());
            if (pending != null) {
                // retransmit is allowed only when status is PUBREC
                pending.getTimeout().cancel();
                pending.setPacket(packet);
            } else {
                pending = MqttQos2RxMessage.builder()
                    .packet(packet)
                    .build();
                pendingRxQos2Messages.put(packet.getPacketId(), pending);
            }
            pubRec(packet.getPacketId());
            pending.setTimeout(timer.newTimeout(timeout -> throwException(new MqttClientException("PUBREL timeout")),
                connectionOption.getPubRelTimeout(), TimeUnit.SECONDS));
        }

        private void handleSubAck(MqttV311SubAckPacket packet) {
            MqttPendingSubscription pending = pendingSubscriptions.remove(packet.getPacketId());
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

                pending.getMqttSubscribeResultHandler().onSuccess(MqttConnection.this, subscriptionList);
            } else {
                throwException(new MqttClientException("invalid packetId in SUBACK packet, pending subscription not found"));
            }
        }

        private void handleConnAck(MqttV311ConnAckPacket packet) {
            log.debug("handle CONACK");
            channel.pipeline().remove(MqttClientConstants.MQTT_CONNECT_TIMER_NAME);
            if (packet.getConnectReturnCode() == MqttV311ConnectReturnCode.CONNECTION_ACCEPTED) {
                if (connectionOption.getKeepAliveSeconds() > 0) {
                    // 添加IdleStateHandler,当连接处于idle状态时间超过设定，会发送userEvent，触发心跳协议处理
                    channel.pipeline().addBefore(MqttClientConstants.MQTT_CLIENT_HANDLER_NAME, MqttClientConstants.MQTT_KEEPALIVE_HANDLER_NAME,
                        new IdleStateHandler(0, (int) connectionOption.getKeepAliveSeconds(), 0));
                }
                status = Status.CONNECTED;
                mqttConnectResultHandler.onSuccess(MqttConnection.this);
            } else {
                channel.close();
                mqttConnectResultHandler.onError(MqttConnection.this, new MqttConnectionException(packet.getConnectReturnCode()));
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

            MqttPendingUnsubscription pending = pendingUnsubscribes.remove(packet.getPacketId());
            if (pending != null) {
                for (String topicFilter : pending.getTopicFilters()) {
                    subscriptionTree.removeSubscription(topicFilter);
                }
                pending.getUnsubscribeResultHandler().onSuccess(MqttConnection.this);
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
                mqttConnectResultHandler.onTimeout(MqttConnection.this);
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
            if (status != Status.CLOSED) {
                connectionOption.getExceptionHandler().onException(MqttConnection.this, new MqttClientException("connection closed unexpectedly"));
            }
        }

        private void doConnect(ChannelHandlerContext ctx) {
            log.debug("channel active");

            MqttV311ConnectPacket.MqttV311ConnectPacketBuilder packetBuilder = MqttV311ConnectPacket.builder()
                .username(connectionOption.getUsername())
                .password(connectionOption.getPassword())
                .clientId(connectionOption.getClientId())
                .keepAliveSeconds(((int) connectionOption.getKeepAliveSeconds()))
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
                        close();
                    }
                });
            if (connectionOption.getMqttConnectTimeout() > 0) {
                ctx.pipeline().addBefore(MqttClientConstants.MQTT_CLIENT_HANDLER_NAME, MqttClientConstants.MQTT_CONNECT_TIMER_NAME,
                    new ReadTimeoutHandler(connectionOption.getMqttConnectTimeout(), TimeUnit.MILLISECONDS));
            }
        }

        //----------------------------------------------------------------------------------------------------

        public void connect(TcpConnectResultHandler tcpConnectResultHandler) {
            bootstrap.connect(connectionOption.getHost(), connectionOption.getPort()).addListener(future -> {
                ChannelFuture channelFuture = (ChannelFuture) future;
                if (future.isSuccess()) {
                    channel = channelFuture.channel();
                } else {
                    Throwable cause = future.cause();
                    if (cause instanceof ConnectTimeoutException) {
                        tcpConnectResultHandler.onTimeout(MqttConnection.this);
                    } else {
                        tcpConnectResultHandler.onError(MqttConnection.this, cause);
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
                    mqttSubscribeResultHandler.onError(MqttConnection.this, future.cause());
                }
            });
        }

        public void publish(String topic, MqttV311QosLevel qosLevel, boolean retain, byte[] payload, MqttPublishResultHandler mqttPublishResultHandler) {

            MqttV311PublishPacket.Builder builder = MqttV311PublishPacket.builder()
                .topic(topic)
                .qosLevel(qosLevel)
                .dupFlag(false)
                .retain(retain)
                .payload(payload);
            int packetId;
            if (qosLevel == MqttV311QosLevel.AT_LEAST_ONCE || qosLevel == MqttV311QosLevel.EXACTLY_ONCE) {
                packetId = nextPacketId();
                builder.packetId(packetId);
            } else {
                packetId = 0;
            }

            log.debug("new publish packetId {}", packetId);
            MqttV311PublishPacket packet = builder.build();
            channel.writeAndFlush(packet).addListener(future -> {
                if (future.isSuccess()) {
                    switch (qosLevel) {
                        case AT_MOST_ONCE:
                            if (mqttPublishResultHandler != null) {
                                mqttPublishResultHandler.onSuccess(MqttConnection.this);
                            }
                            break;
                        case AT_LEAST_ONCE:
                            pendingTxQos1Messages.put(packetId, MqttQos1TxMessage.builder()
                                .packet(packet)
                                .publishResultHandler(mqttPublishResultHandler)
                                .timeout(timer.newTimeout(timeout -> retransmitQos1(packetId),
                                    connectionOption.getPubAckTimeout(), TimeUnit.SECONDS)
                                )
                                .build()
                            );
                            break;
                        case EXACTLY_ONCE:
                            pendingTxQos2Messages.put(packetId, MqttQos2TxMessage.builder()
                                .packet(packet)
                                .publishResultHandler(mqttPublishResultHandler)
                                .status(MqttQos2TxMessage.Status.PUBLISH)
                                .timeout(timer.newTimeout(timeout -> retransmitQos2(packetId),
                                    connectionOption.getPubRecTimeout(), TimeUnit.SECONDS)
                                )
                                .build()
                            );
                            break;
                    }
                } else {
                    mqttPublishResultHandler.onError(MqttConnection.this, future.cause());
                }

            });
        }

        private void retransmitQos1(int packetId) {
            MqttQos1TxMessage pending = pendingTxQos1Messages.get(packetId);
            if (pending != null) {
                int retransmitCount = pending.getRetransmitCount();
                if (retransmitCount < connectionOption.getRetransmitMax()) {
                    channel.writeAndFlush(makeDupPacket(pending.getPacket()));
                    pending.setTimeout(timer.newTimeout(timeout2 -> retransmitQos1(packetId),
                        connectionOption.getPubAckTimeout(), TimeUnit.SECONDS));
                    pending.setRetransmitCount(retransmitCount + 1);
                } else {
                    pending.getPublishResultHandler().onError(MqttConnection.this, new MqttClientException("QoS 1 retransmit max reached"));
                }

            }
        }

        private void retransmitQos2(int packetId) {
            MqttQos2TxMessage pending = pendingTxQos2Messages.get(packetId);
            if (pending != null ) {
                int retransmitCount = pending.getRetransmitCount();
                if (retransmitCount < connectionOption.getRetransmitMax()) {
                    channel.writeAndFlush(makeDupPacket(pending.getPacket()));
                    pending.setTimeout(timer.newTimeout(timeout2 -> retransmitQos2(packetId),
                        connectionOption.getPubRecTimeout(), TimeUnit.SECONDS));
                    pending.setRetransmitCount(retransmitCount + 1);
                } else {
                    pending.getPublishResultHandler().onError(MqttConnection.this, new MqttClientException("QoS 2 retransmit max reached"));
                }
            }
        }

        private MqttV311PublishPacket makeDupPacket(MqttV311PublishPacket packet) {
            return MqttV311PublishPacket.builder()
                .topic(packet.getTopic())
                .retain(packet.isRetain())
                .qosLevel(packet.getQosLevel())
                .payload(packet.getPayload())
                .packetId(packet.getPacketId())
                .dupFlag(true)
                .build();
        }

        private void pingReq(Channel channel) {
            if (this.pingRespTimeoutFuture == null) {
                this.pingRespTimeoutFuture = channel.eventLoop().schedule(() -> {
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

        /**
         * close connection
         */
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
                    pendingUnsubscribes.put(packetId, new MqttPendingUnsubscription(topicFilters, unsubscribeResultHandler));
                } else {
                    unsubscribeResultHandler.onError(MqttConnection.this, future.cause());
                }
            });
        }

        public void pubAck(int packetId) {
            MqttV311PubAckPacket packet = MqttV311PubAckPacket.builder().packetId(packetId).build();
            channel.writeAndFlush(packet).addListener(future -> {
                if (!future.isSuccess()) {
                    throwException(future.cause());
                }
            });
        }

        public void pubRec(int packetId) {
            MqttV311PubRecPacket packet = MqttV311PubRecPacket.builder().packetId(packetId).build();
            channel.writeAndFlush(packet).addListener(future -> {
                if (!future.isSuccess()) {
                    throwException(future.cause());
                }
            });
        }

        public void pubRel(int packetId) {
            MqttV311PubRelPacket packet = MqttV311PubRelPacket.builder().packetId(packetId).build();
            channel.writeAndFlush(packet).addListener(future -> {
                if (!future.isSuccess()) {
                    throwException(future.cause());
                }
            });
        }

        public void pubComp(int packetId) {
            MqttV311PubCompPacket packet = MqttV311PubCompPacket.builder().packetId(packetId).build();
            channel.writeAndFlush(packet).addListener(future -> {
                if (!future.isSuccess()) {
                    throwException(future.cause());
                }
            });
        }

        /**
         * called when exception happens
         *
         * @param cause cause
         */
        private void throwException(Throwable cause) {
            disconnect();
            connectionOption.getExceptionHandler().onException(MqttConnection.this, cause);
        }

        /**
         * send DISCONNECT packet to server and close connection
         */
        public void disconnect() {
            channel.writeAndFlush(MqttV311DisconnectPacket.INSTANCE);
            close();
        }
    }
}
