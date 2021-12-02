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

import io.rapidw.mqtt.client.v3_1_1.MqttClientException;
import io.rapidw.mqtt.client.v3_1_1.MqttConnection;

public interface MqttConnectResultHandler {
    /**
     * will be called when MQTT connection successfully established
     * @param connection the mqtt connection
     */
    void onSuccess(MqttConnection connection);

    /**
     * will be called when error occurred during establishing MQTT connection
     * @param connection the mqtt connection
     * @param cause cause
     */
    void onError(MqttConnection connection, MqttClientException cause);

    /**
     * will be called when establishing MQTT connection timeout
     * @param connection the mqtt connection
     */
    void onTimeout(MqttConnection connection);
}
