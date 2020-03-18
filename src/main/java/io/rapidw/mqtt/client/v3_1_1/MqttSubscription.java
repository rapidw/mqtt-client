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
package io.rapidw.mqtt.client.v3_1_1;

import io.rapidw.mqtt.client.v3_1_1.handler.MqttMessageHandler;
import io.rapidw.mqtt.client.v3_1_1.handler.MqttUnsubscribeResultHandler;
import io.rapidw.mqtt.codec.v3_1_1.MqttV311QosLevel;

public class MqttSubscription {

    private MqttConnection connection;
    private String topicFilter;
    private MqttV311QosLevel qosLevel;
    private MqttMessageHandler messageHandler;

    MqttSubscription(MqttConnection connection, String topicFilter, MqttV311QosLevel qosLevel, MqttMessageHandler messageHandler) {
        this.connection = connection;
        this.topicFilter = topicFilter;
        this.qosLevel = qosLevel;
        this.messageHandler = messageHandler;
    }

    static MqttSubscriptionBuilder builder() {
        return new MqttSubscriptionBuilder();
    }


    /**
     * unsubscribe this subscription
     * @param unsubscribeResultHandler handler for unsubscription result
     */
    public void unsubscribe(MqttUnsubscribeResultHandler unsubscribeResultHandler) {
        connection.unsubscribe(this, unsubscribeResultHandler);
    }

    MqttConnection getConnection() {
        return this.connection;
    }

    String getTopicFilter() {
        return this.topicFilter;
    }

    MqttV311QosLevel getQosLevel() {
        return this.qosLevel;
    }

    MqttMessageHandler getMessageHandler() {
        return this.messageHandler;
    }

    static class MqttSubscriptionBuilder {
        private MqttConnection connection;
        private String topicFilter;
        private MqttV311QosLevel qosLevel;
        private MqttMessageHandler messageHandler;

        MqttSubscriptionBuilder() {
        }

        MqttSubscription.MqttSubscriptionBuilder connection(MqttConnection connection) {
            this.connection = connection;
            return this;
        }

        MqttSubscription.MqttSubscriptionBuilder topicFilter(String topicFilter) {
            this.topicFilter = topicFilter;
            return this;
        }

        MqttSubscription.MqttSubscriptionBuilder qosLevel(MqttV311QosLevel qosLevel) {
            this.qosLevel = qosLevel;
            return this;
        }

        MqttSubscription.MqttSubscriptionBuilder messageHandler(MqttMessageHandler messageHandler) {
            this.messageHandler = messageHandler;
            return this;
        }

        MqttSubscription build() {
            return new MqttSubscription(connection, topicFilter, qosLevel, messageHandler);
        }

        public String toString() {
            return "MqttSubscription.MqttSubscriptionBuilder(connection=" + this.connection + ", topicFilter=" + this.topicFilter + ", qosLevel=" + this.qosLevel + ", messageHandler=" + this.messageHandler + ")";
        }
    }
}
