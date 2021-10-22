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
package io.rapidw.mqtt.client.v3_1_1.handler;

import io.rapidw.mqtt.client.v3_1_1.MqttConnection;
import io.rapidw.mqtt.codec.v3_1_1.MqttV311QosLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface MqttMessageHandler {
    /**
     * will be called when new message from subscribed topic received
     * @param connection the mqtt connection
     * @param topic topic
     * @param qos QoS level
     * @param retain retain
     * @param dupFlag dup for Qos 1 or 2. For QoS 0, this parameter should be ignored
     * @param packetId packetId for QoS 1 or 2. For QoS 0, this parameter should be ignored
     * @param payload payload
     */
    void onMessage(MqttConnection connection, String topic, MqttV311QosLevel qos, boolean retain, boolean dupFlag, Integer packetId, byte[] payload);

    /**
     * do nothing when message received, just print log
     */
    MqttMessageHandler NoOpHandler = new MqttMessageHandler() {
        Logger logger = LoggerFactory.getLogger(getClass());
        @Override
        public void onMessage(MqttConnection connection, String topic, MqttV311QosLevel qos, boolean retain, boolean dupFlag, Integer packetId, byte[] payload) {
            logger.warn("No handler to handle data, topic: {}", topic);
        }
    };
}
