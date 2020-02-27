package io.rapidw.mqtt.client.handler;


public interface MqttConnectResultHandler {
    void onSuccess();
    void onError(Throwable cause);
    void onTimeout();
}
