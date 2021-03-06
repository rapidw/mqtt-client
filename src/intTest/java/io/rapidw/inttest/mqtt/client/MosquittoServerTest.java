/*
 * Copyright 2020 Rapidw
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.rapidw.inttest.mqtt.client;

import io.rapidw.mqtt.client.v3_1_1.MqttV311Client;
import io.rapidw.mqtt.client.v3_1_1.MqttConnection;
import io.rapidw.mqtt.client.v3_1_1.MqttConnectionOption;
import io.rapidw.mqtt.client.v3_1_1.MqttSubscription;
import io.rapidw.mqtt.client.v3_1_1.handler.MqttConnectResultHandler;
import io.rapidw.mqtt.client.v3_1_1.handler.MqttMessageHandler;
import io.rapidw.mqtt.client.v3_1_1.handler.MqttSubscribeResultHandler;
import io.rapidw.mqtt.client.v3_1_1.handler.TcpConnectResultHandler;
import io.rapidw.mqtt.codec.v3_1_1.MqttV311QosLevel;
import io.rapidw.mqtt.codec.v3_1_1.MqttV311TopicAndQosLevel;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
public class MosquittoServerTest {

    @Test
    public void testMqsquittoServer() {
        MqttConnectionOption connectionOption = MqttConnectionOption.builder()
            .cleanSession(true)
            .clientId("test-wafer")
            .host("test.mosquitto.org")
            .port(1883)
            .keepAlive(30)
            .keepAliveOffsetTimeUnit(TimeUnit.SECONDS)
            .keepAliveOffset(2)
            .keepAliveOffsetTimeUnit(TimeUnit.SECONDS)
            .tcpConnectTimeout(1000)
            .mqttConnectTimeout(2000)
            .exceptionHandler((connection, e) -> log.error("error", e))
            .build();
        MqttV311Client mqttV311Client = new MqttV311Client();
        MqttConnection connection = mqttV311Client.newConnection(connectionOption);

        MqttMessageHandler mqttMessageHandler = (connection1, topic, qos, retain, dupFlag, packetId, payload) -> {
            log.info("topic: {}", topic);
        };


        connection.connect(new TcpConnectResultHandler() {
            @Override
            public void onSuccess(MqttConnection connection1) {
                log.info("tcp connect success");
            }

            @Override
            public void onError(MqttConnection connection1, Throwable throwable) {
                log.info("tcp connect error");
            }

            @Override
            public void onTimeout(MqttConnection connection1) {
                log.info("tcp connect timeout");
            }
        }, new MqttConnectResultHandler() {
            @Override
            public void onError(MqttConnection connection1, Throwable cause) {
                log.error("mqtt connect error", cause);
            }

            @Override
            public void onSuccess(MqttConnection connection1) {
                log.info("mqtt connect success");
                connection.subscribe(Collections.singletonList(new MqttV311TopicAndQosLevel("#", MqttV311QosLevel.AT_MOST_ONCE)), mqttMessageHandler, new MqttSubscribeResultHandler() {
                    @Override
                    public void onSuccess(MqttConnection connection1, List<MqttSubscription> subscriptions) {
                        log.info("mqtt subscribe success");
//                        connection.close();
                    }

                    @Override
                    public void onError(MqttConnection connection1, Throwable cause) {
                        log.info("onError", cause);
                    }
                });

            }

            @Override
            public void onTimeout(MqttConnection connection1) {
                log.info("mqtt connect timeout");
            }
        });

        connection.waitForClose();
    }
}
