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
package io.rapidw.mqtt.client.v3_1_1

import io.rapidw.mqtt.client.v3_1_1.handler.MqttMessageHandler
import io.rapidw.mqtt.codec.v3_1_1.MqttV311QosLevel
import io.rapidw.mqtt.codec.v3_1_1.MqttV311TopicAndQosLevel
import spock.lang.Specification


class MqttTopicTreeSpec extends Specification {

    def "subscribe"() {

        when:
        def topicTree = new MqttTopicTree()
        def handler1 = Mock(MqttMessageHandler.class)
        def handler2 = Mock(MqttMessageHandler.class)
        topicTree.addSubscription(new MqttV311TopicAndQosLevel("#", MqttV311QosLevel.AT_MOST_ONCE), handler1)
        topicTree.addSubscription(new MqttV311TopicAndQosLevel("/MQTTHome/Reply", MqttV311QosLevel.AT_LEAST_ONCE), handler2)

        then:
        topicTree.getHandlersByTopicName("abcde") ==~ [handler1]
        topicTree.getHandlersByTopicName("/MQTTHome/Reply") ==~ [handler2]
    }

}
