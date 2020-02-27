package io.rapidw.mqtt.client.handler;

public interface MqttExceptionHandler {
    void onException(Throwable e);
}
