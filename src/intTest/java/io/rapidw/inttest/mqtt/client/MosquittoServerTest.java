package io.rapidw.inttest.mqtt.client;

import io.rapidw.mqtt.client.MqttClient;
import io.rapidw.mqtt.client.MqttConnection;
import io.rapidw.mqtt.client.MqttConnectionOption;
import io.rapidw.mqtt.client.MqttSubscription;
import io.rapidw.mqtt.client.handler.MqttConnectResultHandler;
import io.rapidw.mqtt.client.handler.MqttMessageHandler;
import io.rapidw.mqtt.client.handler.MqttSubscribeResultHandler;
import io.rapidw.mqtt.client.handler.TcpConnectResultHandler;
import io.rapidw.mqtt.codec.MqttQosLevel;
import io.rapidw.mqtt.codec.MqttTopicAndQosLevel;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

@Slf4j
public class MosquittoServerTest {

    @Test
    @SneakyThrows
    public void testMqsquittoServer() {
        MqttConnectionOption connectionOption = MqttConnectionOption.builder()
            .cleanSession(true)
            .clientId("test-wafer")
            .host("test.mosquitto.org")
            .port(1883)
            .keepAliveSeconds(30)
            .keepAliveSecondsOffset(2)
            .tcpConnectTimeout(1000)
            .mqttConnectTimeout(2000)
            .exceptionHandler(cause -> log.error("error", cause))
            .build();
        MqttClient mqttClient = new MqttClient();
        MqttConnection connection = mqttClient.newConnection(connectionOption);

        MqttMessageHandler mqttMessageHandler = (topic, qos, retain, dupFlag, packetId, payload) -> {
            log.info("topic: {}", topic);
        };


        connection.connect(new TcpConnectResultHandler() {
            @Override
            public void onSuccess() {
                log.info("tcp connect success");
            }

            @Override
            public void onError(Throwable throwable) {
                log.info("tcp connect error");
            }

            @Override
            public void onTimeout() {
                log.info("tcp connect timeout");
            }
        }, new MqttConnectResultHandler() {
            @Override
            public void onError(Throwable cause) {
                log.error("mqtt connect error", cause);
            }

            @Override
            public void onSuccess() {
                log.info("mqtt connect success");
                connection.subscribe(Collections.singletonList(new MqttTopicAndQosLevel("#", MqttQosLevel.AT_MOST_ONCE)), mqttMessageHandler, new MqttSubscribeResultHandler() {
                    @Override
                    public void onSuccess(List<MqttSubscription> subscriptions) {
                        log.info("mqtt subscribe success");
//                        connection.close();
                    }

                    @Override
                    public void onError(Throwable cause) {
                        log.info("onError", cause);
                    }
                });

            }

            @Override
            public void onTimeout() {
                log.info("mqtt connect timeout");
            }
        });

        connection.waitForClose();
    }
}
