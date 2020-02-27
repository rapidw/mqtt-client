package io.rapidw.mqtt.client;

import io.rapidw.mqtt.client.handler.MqttUnsubscribeResultHandler;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@AllArgsConstructor
@Getter
class MqttPendingUnsubscription {

    private List<String> topicFilters;
    private MqttUnsubscribeResultHandler unsubscribeResultHandler;
}
