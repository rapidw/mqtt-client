package io.rapidw.mqtt.client.handler;

import io.rapidw.mqtt.codec.MqttQosLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface MqttMessageHandler {
    void onMessage(String topic, MqttQosLevel qos, boolean retain, boolean dupFlag, int packetId, byte[] payload);

    MqttMessageHandler NoOpHandler = new MqttMessageHandler() {
        Logger logger = LoggerFactory.getLogger(getClass());
        @Override
        public void onMessage(String topic, MqttQosLevel qos, boolean retain, boolean dupFlag, int packetId, byte[] payload) {
            logger.warn("No handler to handle data, topic: {}", topic);
        }
    };
}
