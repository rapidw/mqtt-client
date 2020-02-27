package io.rapidw.mqtt.client;

import io.rapidw.mqtt.client.handler.MqttMessageHandler;
import io.rapidw.mqtt.codec.MqttQosLevel;
import io.rapidw.mqtt.codec.MqttTopicAndQosLevel;
import lombok.val;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;

public class MqttTopicTreeTest {

    @Test
    public void testTopicTreeSubscribe() {
        val topicTree = new MqttTopicTree();

        val handler1 = mock(MqttMessageHandler.class);
        val handler2 = mock(MqttMessageHandler.class);
        topicTree.addSubscription(new MqttTopicAndQosLevel("#", MqttQosLevel.AT_MOST_ONCE), handler1);
        topicTree.addSubscription(new MqttTopicAndQosLevel("/MQTTHome/Reply", MqttQosLevel.AT_LEAST_ONCE), handler2);

        Assertions.assertThat(topicTree.getHandlersByTopicName("abcde")).containsOnly(handler1);
        Assertions.assertThat(topicTree.getHandlersByTopicName("/MQTTHome/Reply")).containsOnly(handler2);
    }
}
