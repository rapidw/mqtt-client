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

import io.netty.util.Timeout;
import io.rapidw.mqtt.client.v3_1_1.handler.MqttPublishResultHandler;
import io.rapidw.mqtt.codec.v3_1_1.MqttV311PublishPacket;
import io.rapidw.mqtt.codec.v3_1_1.MqttV311QosLevel;

class MqttQos1PendingMessage {
    private final MqttV311PublishPacket packet;
    private final MqttPublishResultHandler publishResultHandler;
    private Timeout timeout;

    MqttQos1PendingMessage(MqttV311PublishPacket packet, MqttPublishResultHandler publishResultHandler,
                           Timeout timeout) {
        this.packet = packet;
        this.publishResultHandler = publishResultHandler;
        this.timeout = timeout;
    }

    public static MqttQos1PendingMessageBuilder builder() {
        return new MqttQos1PendingMessageBuilder();
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

    public static class MqttQos1PendingMessageBuilder {
        MqttV311PublishPacket packet;
        MqttPublishResultHandler publishResultHandler;
        Timeout timeout;

        MqttQos1PendingMessageBuilder() {
        }

        public MqttQos1PendingMessage.MqttQos1PendingMessageBuilder packet(MqttV311PublishPacket packet) {
            this.packet = packet;
            return this;
        }

        public MqttQos1PendingMessage.MqttQos1PendingMessageBuilder publishResultHandler(MqttPublishResultHandler publishResultHandler) {
            this.publishResultHandler = publishResultHandler;
            return this;
        }

        public MqttQos1PendingMessage.MqttQos1PendingMessageBuilder timeout(Timeout timeout) {
            this.timeout = timeout;
            return this;
        }

        public MqttQos1PendingMessage build() {
            return new MqttQos1PendingMessage(packet, publishResultHandler, timeout);
        }
    }
}
