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
import io.rapidw.mqtt.codec.v3_1_1.MqttV311PublishPacket;

class MqttQos2RxMessage {

    private MqttV311PublishPacket packet;
    private Timeout timeout;

    MqttQos2RxMessage(MqttV311PublishPacket packet, Timeout timeout) {
        this.packet = packet;
        this.timeout = timeout;
    }

    public void setPacket(MqttV311PublishPacket packet) {
        this.packet = packet;
    }

    public MqttV311PublishPacket getPacket() {
        return this.packet;
    }

    public void setTimeout(Timeout timeout) {
        this.timeout = timeout;
    }

    public Timeout getTimeout() {
        return this.timeout;
    }

    public static MqttQos2RxMessage.Builder builder() {
        return new MqttQos2RxMessage.Builder();
    }

    public static class Builder {
        private MqttV311PublishPacket packet;
        private Timeout timeout;

        public Builder packet(MqttV311PublishPacket packet) {
            this.packet = packet;
            return this;
        }

        public Builder timeout(Timeout timeout) {
            this.timeout = timeout;
            return this;
        }

        public MqttQos2RxMessage build() {
            return new MqttQos2RxMessage(packet, timeout);
        }
    }
}
