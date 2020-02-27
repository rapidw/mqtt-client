package io.rapidw.mqtt.client;

public class MqttClientException extends RuntimeException {

    public MqttClientException(String message) {
        super(message);
    }

    public MqttClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
