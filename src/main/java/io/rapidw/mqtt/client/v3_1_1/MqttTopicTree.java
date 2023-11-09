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
import io.rapidw.mqtt.codec.v3_1_1.MqttV311QosLevel;
import io.rapidw.mqtt.codec.v3_1_1.MqttV311TopicAndQosLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

class MqttTopicTree {
    private static Logger log = LoggerFactory.getLogger(MqttTopicTree.class);

    private TopicNode root = TopicNode.newRootNode();

    public synchronized void addSubscription(MqttV311TopicAndQosLevel topicAndQosLevel, MqttMessageHandler messageHandler) {
        TopicNode currentNode = root;
        log.debug("add sub parts: {}", splitTopic(topicAndQosLevel.getTopicFilter()));
        List<String> parts = splitTopic(topicAndQosLevel.getTopicFilter());
        int size = parts.size();
        for (int i = 0; i < size; i++) {
            String level = parts.get(i);
            if (i == (size - 1)) {
                currentNode.addChild(level, topicAndQosLevel.getQosLevel(), messageHandler);
            } else {
                currentNode = currentNode.addChild(level, null, messageHandler);
            }
        }
    }

    public Set<MqttMessageHandler> getHandlersByTopicName(String topicName) {
        List<TopicNode> nodes = new LinkedList<>();
        TopicNode currentNode = root;
        TopicNode nextNode;
        for (String level : splitTopic(topicName)) {
            nextNode = currentNode.getChild(level);
            if (nextNode == null) {
                TopicNode node = currentNode.getChild("#");
                if (node != null) {
                    log.debug("current level match multi");
                    nodes.add(node);
                }
                node = currentNode.getChild("+");
                if (node != null) {
                    currentNode = node;
                }
            } else {
                currentNode = nextNode;
            }
        }
        if (currentNode != null && currentNode != root) {
            nodes.add(currentNode);
        }

        Set<MqttMessageHandler> qos0Handlers = new HashSet<>();
        Set<MqttMessageHandler> qos1Handlers = new HashSet<>();
        Set<MqttMessageHandler> qos2Handlers = new HashSet<>();

        boolean hasQos2 = false;
        boolean hasQos1 = false;

        for (TopicNode node : nodes) {
            if (node.qos2Handlers.size() != 0) {
                qos2Handlers.addAll(node.qos2Handlers);
                hasQos2 = true;
            }
            if (!hasQos2 && node.qos1Handlers.size() != 0) {
                qos1Handlers.addAll(node.qos1Handlers);
                hasQos1 = true;
            }
            if (!hasQos2 && !hasQos1 && node.qos0Handlers.size() != 0) {
                qos0Handlers.addAll(node.qos0Handlers);
            }
        }
        if (hasQos2) {
            return qos2Handlers;
        }
        if (qos1Handlers.size() != 0) {
            return qos1Handlers;
        }
        return qos0Handlers;

    }

    private static List<String> splitTopic(String topic) {
        List<String> strings = new LinkedList<>();
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < topic.length(); i++) {
            char c = topic.charAt(i);
            if (c != '/') {
                builder.append(c);
            } else {
                strings.add(builder.toString());
                builder = new StringBuilder();
            }
        }
        strings.add(builder.toString());
        return strings;
    }

    public synchronized void removeSubscription(String topicFilter) {

    }

    public static class TopicNode {

        private Set<MqttMessageHandler> qos0Handlers;
        private Set<MqttMessageHandler> qos1Handlers;
        private Set<MqttMessageHandler> qos2Handlers;
        private Map<String, TopicNode> children = new HashMap<>();

        static TopicNode newRootNode() {
            return new TopicNode();
        }

        public TopicNode addChild(String name, MqttV311QosLevel qosLevel, MqttMessageHandler handler) {

            TopicNode child = children.get(name);
            if (child != null) {
                if (qosLevel != null) {
                    if (qosLevel == MqttV311QosLevel.EXACTLY_ONCE && !child.getQos2Handlers().contains(handler)) {
                        child.getQos2Handlers().add(handler);
                    } else if (qosLevel == MqttV311QosLevel.AT_LEAST_ONCE
                        && child.getQos2Handlers().isEmpty()
                        && !child.getQos1Handlers().contains(handler)) {
                        child.getQos1Handlers().add(handler);
                    } else if (qosLevel == MqttV311QosLevel.AT_MOST_ONCE
                        && child.getQos2Handlers().isEmpty()
                        && child.getQos1Handlers().isEmpty()
                        && child.getQos0Handlers().contains(handler)) {
                        child.getQos0Handlers().add(handler);
                    }
                }
                return child;
            } else {
                TopicNode node = new TopicNode();
                node.qos0Handlers = new HashSet<>();
                node.qos1Handlers = new HashSet<>();
                node.qos2Handlers = new HashSet<>();
                if (qosLevel != null) {
                    switch (qosLevel) {
                        case AT_MOST_ONCE:
                            node.qos0Handlers.add(handler);
                            break;
                        case AT_LEAST_ONCE:
                            node.qos1Handlers.add(handler);
                            break;
                        case EXACTLY_ONCE:
                            node.qos2Handlers.add(handler);
                            break;
                    }
                }
                this.children.put(name, node);
                return node;
            }
        }

        public TopicNode getChild(String name) {
            return children.get(name);
        }

        Set<MqttMessageHandler> getQos0Handlers() {
            return this.qos0Handlers;
        }

        Set<MqttMessageHandler> getQos1Handlers() {
            return this.qos1Handlers;
        }

        Set<MqttMessageHandler> getQos2Handlers() {
            return this.qos2Handlers;
        }

        public Map<String, TopicNode> getChildren() {
            return this.children;
        }
    }
}
