package io.rapidw.mqtt.client.v3_1_1;

import io.netty.util.Timeout;
import io.rapidw.mqtt.client.v3_1_1.handler.MqttPublishResultHandler;
import io.rapidw.mqtt.codec.v3_1_1.MqttV311PublishPacket;

public class MqttQos2PendingMessage {

    private MqttV311PublishPacket packet;
    private final MqttPublishResultHandler publishResultHandler;
    private Timeout timeout;

    private Status status;
    public enum Status {
        PUBLISH,
        PUBREC,
        PUBREL,
    }

    MqttQos2PendingMessage(MqttV311PublishPacket packet, MqttPublishResultHandler publishResultHandler,
                           Timeout timeout, Status status) {
        this.packet = packet;
        this.publishResultHandler = publishResultHandler;
        this.timeout = timeout;
        this.status = status;
    }

    public void setPacket(MqttV311PublishPacket packet) {
        this.packet = packet;
    }

    public MqttV311PublishPacket getPacket() {
        return this.packet;
    }

    public MqttPublishResultHandler getPublishResultHandler() {
        return this.publishResultHandler;
    }

    public void setTimeout(Timeout timeout) {
        this.timeout = timeout;
    }

    public Timeout getTimeout() {
        return this.timeout;
    }

    public Status getStatus() {
        return this.status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public static MqttQos2PendingMessage.MqttQos2PendingMessageBuilder builder() {
        return new MqttQos2PendingMessage.MqttQos2PendingMessageBuilder();
    }

    public static class MqttQos2PendingMessageBuilder {
        private MqttV311PublishPacket packet;
        private MqttPublishResultHandler publishResultHandler;
        private Timeout timeout;
        private Status status;

        public MqttQos2PendingMessage.MqttQos2PendingMessageBuilder packet(MqttV311PublishPacket packet) {
            this.packet = packet;
            return this;
        }

        public MqttQos2PendingMessage.MqttQos2PendingMessageBuilder publishResultHandler(MqttPublishResultHandler publishResultHandler) {
            this.publishResultHandler = publishResultHandler;
            return this;
        }

        public MqttQos2PendingMessage.MqttQos2PendingMessageBuilder timeout(Timeout timeout) {
            this.timeout = timeout;
            return this;
        }

        public MqttQos2PendingMessage.MqttQos2PendingMessageBuilder status(Status status) {
            this.status = status;
            return this;
        }

        public MqttQos2PendingMessage build() {
            return new MqttQos2PendingMessage(packet, publishResultHandler, timeout, status);
        }
    }
}
