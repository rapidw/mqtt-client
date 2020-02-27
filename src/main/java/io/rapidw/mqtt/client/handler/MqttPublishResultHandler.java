package io.rapidw.mqtt.client.handler;

public interface MqttPublishResultHandler {

    void onSuccess();
    void onError(Throwable cause);
}
