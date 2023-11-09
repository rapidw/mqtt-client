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

import io.rapidw.mqtt.client.v3_1_1.handler.MqttUnsubscribeResultHandler;

import java.util.List;

class MqttPendingUnsubscription {

    private List<String> topicFilters;
    private MqttUnsubscribeResultHandler unsubscribeResultHandler;

    public MqttPendingUnsubscription(List<String> topicFilters, MqttUnsubscribeResultHandler unsubscribeResultHandler) {
        this.topicFilters = topicFilters;
        this.unsubscribeResultHandler = unsubscribeResultHandler;
    }

    public List<String> getTopicFilters() {
        return this.topicFilters;
    }

    public MqttUnsubscribeResultHandler getUnsubscribeResultHandler() {
        return this.unsubscribeResultHandler;
    }
}
