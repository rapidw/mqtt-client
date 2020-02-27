package io.rapidw.mqtt.client.handler;

import io.rapidw.mqtt.client.MqttSubscription;

import java.util.List;

public interface MqttSubscribeResultHandler {
    void onSuccess(List<MqttSubscription> subscriptions);
    void onError(Throwable cause);
}
