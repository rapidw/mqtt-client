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
package io.rapidw.mqtt.client.v3_1_1.handler;

import io.rapidw.mqtt.client.v3_1_1.MqttConnection;
import io.rapidw.mqtt.client.v3_1_1.MqttSubscription;

import java.util.List;

public interface MqttSubscribeResultHandler {
    /**
     * will be called when a topic subscribed successfully
     * @param connection the mqtt connection
     * @param subscriptions subscribed subscriptions
     */
    void onSuccess(MqttConnection connection, List<MqttSubscription> subscriptions);

    /**
     * will be called when error occurred during subscribing a topic
     * @param connection the mqtt connection
     * @param cause cause
     */
    void onError(MqttConnection connection, Throwable cause);
}
