package io.rapidw.mqtt.client;

import io.rapidw.mqtt.codec.MqttConnectReturnCode;


public class MqttConnectionException extends MqttClientException {

    private MqttConnectReturnCode connectReturnCode;

    public MqttConnectionException(MqttConnectReturnCode connectReturnCode) {
        super("connect failed, return code is " + connectReturnCode.name());
    }

    public MqttConnectReturnCode getConnectReturnCode() {
        return connectReturnCode;
    }
}
