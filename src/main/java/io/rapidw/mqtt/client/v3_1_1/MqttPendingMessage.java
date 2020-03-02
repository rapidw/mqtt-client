package io.rapidw.mqtt.client.v3_1_1;

import io.rapidw.mqtt.client.v3_1_1.handler.MqttPublishResultHandler;
import io.rapidw.mqtt.codec.v3_1_1.MqttV311PublishPacket;

public class MqttPendingMessage {
    private MqttV311PublishPacket packet;
    private MqttPublishResultHandler publishResultHandler;

    MqttPendingMessage(MqttV311PublishPacket packet, MqttPublishResultHandler publishResultHandler) {
        this.packet = packet;
        this.publishResultHandler = publishResultHandler;
    }

    public static MqttPendingMessageBuilder builder() {
        return new MqttPendingMessageBuilder();
    }

    public MqttV311PublishPacket getPacket() {
        return this.packet;
    }

    public MqttPublishResultHandler getPublishResultHandler() {
        return this.publishResultHandler;
    }

    public static class MqttPendingMessageBuilder {
        private MqttV311PublishPacket packet;
        private MqttPublishResultHandler publishResultHandler;

        MqttPendingMessageBuilder() {
        }

        public MqttPendingMessage.MqttPendingMessageBuilder packet(MqttV311PublishPacket packet) {
            this.packet = packet;
            return this;
        }

        public MqttPendingMessage.MqttPendingMessageBuilder publishResultHandler(MqttPublishResultHandler publishResultHandler) {
            this.publishResultHandler = publishResultHandler;
            return this;
        }

        public MqttPendingMessage build() {
            return new MqttPendingMessage(packet, publishResultHandler);
        }

        public String toString() {
            return "MqttPendingMessage.MqttPendingMessageBuilder(packet=" + this.packet + ", publishResultHandler=" + this.publishResultHandler + ")";
        }
    }
}
