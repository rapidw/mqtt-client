package io.rapidw.mqtt.client;

import io.rapidw.mqtt.client.handler.MqttMessageHandler;
import io.rapidw.mqtt.client.handler.MqttSubscribeResultHandler;
import io.rapidw.mqtt.codec.MqttTopicAndQosLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;

import java.util.List;

@Builder
@Getter
class MqttPendingSubscription {

    private MqttMessageHandler messageHandler;
    @Singular
    private List<MqttTopicAndQosLevel> topicAndQosLevels;
    private MqttSubscribeResultHandler mqttSubscribeResultHandler;
}
