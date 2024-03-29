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

class MqttQos1TxMessage {
    private final MqttV311PublishPacket packet;
    private final MqttPublishResultHandler publishResultHandler;
    private Timeout timeout;
    private int retransmitCount;

    MqttQos1TxMessage(MqttV311PublishPacket packet, MqttPublishResultHandler publishResultHandler,
                      Timeout timeout) {
        this.packet = packet;
        this.publishResultHandler = publishResultHandler;
        this.timeout = timeout;
    }

    public static Builder builder() {
        return new Builder();
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

    public int getRetransmitCount() {
        return this.retransmitCount;
    }

    public void setRetransmitCount(int retransmitCount) {
        this.retransmitCount = retransmitCount;
    }

    public static class Builder {
        MqttV311PublishPacket packet;
        MqttPublishResultHandler publishResultHandler;
        Timeout timeout;

        Builder() {
        }

        public Builder packet(MqttV311PublishPacket packet) {
            this.packet = packet;
            return this;
        }

        public Builder publishResultHandler(MqttPublishResultHandler publishResultHandler) {
            this.publishResultHandler = publishResultHandler;
            return this;
        }

        public Builder timeout(Timeout timeout) {
            this.timeout = timeout;
            return this;
        }

        public MqttQos1TxMessage build() {
            return new MqttQos1TxMessage(packet, publishResultHandler, timeout);
        }
    }
}
