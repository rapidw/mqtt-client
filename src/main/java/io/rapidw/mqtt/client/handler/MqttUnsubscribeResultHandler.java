package io.rapidw.mqtt.client.handler;

public interface MqttUnsubscribeResultHandler {
    void onSuccess();
    void onError(Throwable cause);
}
