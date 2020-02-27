package io.rapidw.mqtt.client;

import io.rapidw.mqtt.client.handler.MqttMessageHandler;
import io.rapidw.mqtt.client.handler.MqttUnsubscribeResultHandler;
import io.rapidw.mqtt.codec.MqttQosLevel;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;

@Builder(access = AccessLevel.PACKAGE)
@Getter(AccessLevel.PACKAGE)
public class MqttSubscription {

    private MqttConnection connection;
    private String topicFilter;
    private MqttQosLevel qosLevel;
    private MqttMessageHandler messageHandler;


    public void unsubscribe(MqttUnsubscribeResultHandler unsubscribeResultHandler) {
        connection.unsubscribe(this, unsubscribeResultHandler);
    }
}
