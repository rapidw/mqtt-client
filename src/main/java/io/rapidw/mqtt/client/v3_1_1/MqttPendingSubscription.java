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

import io.rapidw.mqtt.client.v3_1_1.handler.MqttMessageHandler;
import io.rapidw.mqtt.client.v3_1_1.handler.MqttSubscribeResultHandler;
import io.rapidw.mqtt.codec.v3_1_1.MqttV311TopicAndQosLevel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

class MqttPendingSubscription {

    private MqttMessageHandler messageHandler;
    private List<MqttV311TopicAndQosLevel> topicAndQosLevels;
    private MqttSubscribeResultHandler mqttSubscribeResultHandler;

    MqttPendingSubscription(MqttMessageHandler messageHandler, List<MqttV311TopicAndQosLevel> topicAndQosLevels, MqttSubscribeResultHandler mqttSubscribeResultHandler) {
        this.messageHandler = messageHandler;
        this.topicAndQosLevels = topicAndQosLevels;
        this.mqttSubscribeResultHandler = mqttSubscribeResultHandler;
    }

    public static MqttPendingSubscriptionBuilder builder() {
        return new MqttPendingSubscriptionBuilder();
    }

    public MqttMessageHandler getMessageHandler() {
        return this.messageHandler;
    }

    public List<MqttV311TopicAndQosLevel> getTopicAndQosLevels() {
        return this.topicAndQosLevels;
    }

    public MqttSubscribeResultHandler getMqttSubscribeResultHandler() {
        return this.mqttSubscribeResultHandler;
    }

    public static class MqttPendingSubscriptionBuilder {
        private MqttMessageHandler messageHandler;
        private ArrayList<MqttV311TopicAndQosLevel> topicAndQosLevels;
        private MqttSubscribeResultHandler mqttSubscribeResultHandler;

        MqttPendingSubscriptionBuilder() {
        }

        public MqttPendingSubscription.MqttPendingSubscriptionBuilder messageHandler(MqttMessageHandler messageHandler) {
            this.messageHandler = messageHandler;
            return this;
        }

        public MqttPendingSubscription.MqttPendingSubscriptionBuilder topicAndQosLevel(MqttV311TopicAndQosLevel topicAndQosLevel) {
            if (this.topicAndQosLevels == null)
                this.topicAndQosLevels = new ArrayList<MqttV311TopicAndQosLevel>();
            this.topicAndQosLevels.add(topicAndQosLevel);
            return this;
        }

        public MqttPendingSubscription.MqttPendingSubscriptionBuilder topicAndQosLevels(Collection<? extends MqttV311TopicAndQosLevel> topicAndQosLevels) {
            if (this.topicAndQosLevels == null)
                this.topicAndQosLevels = new ArrayList<MqttV311TopicAndQosLevel>();
            this.topicAndQosLevels.addAll(topicAndQosLevels);
            return this;
        }

        public MqttPendingSubscription.MqttPendingSubscriptionBuilder clearTopicAndQosLevels() {
            if (this.topicAndQosLevels != null)
                this.topicAndQosLevels.clear();
            return this;
        }

        public MqttPendingSubscription.MqttPendingSubscriptionBuilder mqttSubscribeResultHandler(MqttSubscribeResultHandler mqttSubscribeResultHandler) {
            this.mqttSubscribeResultHandler = mqttSubscribeResultHandler;
            return this;
        }

        public MqttPendingSubscription build() {
            List<MqttV311TopicAndQosLevel> topicAndQosLevels;
            switch (this.topicAndQosLevels == null ? 0 : this.topicAndQosLevels.size()) {
                case 0:
                    topicAndQosLevels = java.util.Collections.emptyList();
                    break;
                case 1:
                    topicAndQosLevels = java.util.Collections.singletonList(this.topicAndQosLevels.get(0));
                    break;
                default:
                    topicAndQosLevels = java.util.Collections.unmodifiableList(new ArrayList<MqttV311TopicAndQosLevel>(this.topicAndQosLevels));
            }

            return new MqttPendingSubscription(messageHandler, topicAndQosLevels, mqttSubscribeResultHandler);
        }

        public String toString() {
            return "MqttPendingSubscription.MqttPendingSubscriptionBuilder(messageHandler=" + this.messageHandler + ", topicAndQosLevels=" + this.topicAndQosLevels + ", mqttSubscribeResultHandler=" + this.mqttSubscribeResultHandler + ")";
        }
    }
}
