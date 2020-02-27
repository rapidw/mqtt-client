package io.rapidw.mqtt.client;

import io.rapidw.mqtt.client.handler.MqttMessageHandler;
import io.rapidw.mqtt.codec.MqttQosLevel;
import io.rapidw.mqtt.codec.MqttTopicAndQosLevel;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import lombok.var;

import java.util.*;

@Slf4j
class MqttTopicTree {

    private TopicNode root = TopicNode.newRootNode();

    public synchronized void addSubscription(MqttTopicAndQosLevel topicAndQosLevel, MqttMessageHandler messageHandler) {
        TopicNode currentNode = root;
        log.debug("add sub parts: {}", splitTopic(topicAndQosLevel.getTopicFilter()));
        val parts = splitTopic(topicAndQosLevel.getTopicFilter());
        val size = parts.size();
        for (int i = 0; i < size; i++) {
            val level = parts.get(i);
            if (i == (size - 1)) {
                currentNode.addChild(level, topicAndQosLevel.getQosLevel(), messageHandler);
            } else {
                currentNode = currentNode.addChild(level, null, messageHandler);
            }
        }
    }

    public Set<MqttMessageHandler> getHandlersByTopicName(String topicName) {
        val nodes = new LinkedList<TopicNode>();
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

        val qos0Handlers = new HashSet<MqttMessageHandler>();
        val qos1Handlers = new HashSet<MqttMessageHandler>();
        val qos2Handlers = new HashSet<MqttMessageHandler>();

        var hasQos2 = false;
        var hasQos1 = false;

        for (val node : nodes) {
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

        @Getter(AccessLevel.PACKAGE)
        private Set<MqttMessageHandler> qos0Handlers;
        @Getter(AccessLevel.PACKAGE)
        private Set<MqttMessageHandler> qos1Handlers;
        @Getter(AccessLevel.PACKAGE)
        private Set<MqttMessageHandler> qos2Handlers;
        @Getter
        private Map<String, TopicNode> children = new HashMap<>();

        static TopicNode newRootNode() {
            return new TopicNode();
        }

        public TopicNode addChild(String name, MqttQosLevel qosLevel, MqttMessageHandler handler) {

            val child = children.get(name);
            if (child != null) {
                if (qosLevel != null) {
                    if (qosLevel == MqttQosLevel.EXACTLY_ONCE && !child.getQos2Handlers().contains(handler)) {
                        child.getQos2Handlers().add(handler);
                    } else if (qosLevel == MqttQosLevel.AT_LEAST_ONCE
                        && child.getQos2Handlers().isEmpty()
                        && !child.getQos1Handlers().contains(handler)) {
                        child.getQos1Handlers().add(handler);
                    } else if (qosLevel == MqttQosLevel.AT_MOST_ONCE
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
    }
}
