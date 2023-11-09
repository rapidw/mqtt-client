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

import io.rapidw.mqtt.client.v3_1_1.handler.MqttPublishResultHandler;
import io.rapidw.mqtt.codec.v3_1_1.MqttV311PublishPacket;
import io.rapidw.mqtt.codec.v3_1_1.MqttV311QosLevel;

class MqttPendingMessage {
    private final MqttV311PublishPacket packet;
    private final MqttPublishResultHandler publishResultHandler;
    private final MqttV311QosLevel qosLevel;

    MqttPendingMessage(MqttV311PublishPacket packet, MqttPublishResultHandler publishResultHandler, MqttV311QosLevel qosLevel) {
        this.packet = packet;
        this.publishResultHandler = publishResultHandler;
        this.qosLevel = qosLevel;
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

    public MqttV311QosLevel getQosLevel() {
        return this.qosLevel;
    }

    public static class MqttPendingMessageBuilder {
        private MqttV311PublishPacket packet;
        private MqttPublishResultHandler publishResultHandler;
        private MqttV311QosLevel qosLevel;

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

        public MqttPendingMessage.MqttPendingMessageBuilder qosLevel(MqttV311QosLevel qosLevel) {
            this.qosLevel = qosLevel;
            return this;
        }

        public MqttPendingMessage build() {
            return new MqttPendingMessage(packet, publishResultHandler, qosLevel);
        }

    }
}
