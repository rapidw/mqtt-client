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
package io.rapidw.mqtt.client.inttest

import io.rapidw.mqtt.client.v3_1_1.MqttClientException
import io.rapidw.mqtt.client.v3_1_1.MqttConnection
import io.rapidw.mqtt.client.v3_1_1.MqttConnectionOption
import io.rapidw.mqtt.client.v3_1_1.MqttSubscription
import io.rapidw.mqtt.client.v3_1_1.MqttV311Client
import io.rapidw.mqtt.client.v3_1_1.handler.MqttConnectResultHandler
import io.rapidw.mqtt.client.v3_1_1.handler.MqttMessageHandler
import io.rapidw.mqtt.client.v3_1_1.handler.MqttSubscribeResultHandler
import io.rapidw.mqtt.client.v3_1_1.handler.TcpConnectResultHandler
import io.rapidw.mqtt.codec.v3_1_1.MqttV311QosLevel
import io.rapidw.mqtt.codec.v3_1_1.MqttV311TopicAndQosLevel
import org.junit.jupiter.api.Test
import spock.lang.Specification

import java.util.concurrent.TimeUnit


class MosquittoServerSpec extends Specification {

    def "mosquitto"() {

        MqttConnectionOption connectionOption = MqttConnectionOption.builder()
            .cleanSession(true)
            .clientId("test-wafer")
            .host("test.mosquitto.org")
            .port(1883)
            .keepAliveSeconds(30)
            .tcpConnectTimeout(1, TimeUnit.SECONDS)
            .mqttConnectTimeout(2, TimeUnit.SECONDS)
            .exceptionHandler((connection, e) -> println "error ${e}")
            .build()
        MqttV311Client mqttV311Client = new MqttV311Client()
        MqttConnection connection = mqttV311Client.newConnection(connectionOption)

        MqttMessageHandler mqttMessageHandler = (connection1, topic, qos, retain, dupFlag, packetId, payload) -> {
            println "topic: ${topic}"
        }


        connection.connect(new TcpConnectResultHandler() {
            @Override
             void onSuccess(MqttConnection connection1) {
                println "tcp connect success"
            }

            @Override
             void onError(MqttConnection connection1, Throwable throwable) {
                println "tcp connect error"
            }

            @Override
             void onTimeout(MqttConnection connection1) {
                println "tcp connect timeout"
            }
        }, new MqttConnectResultHandler() {
            @Override
             void onError(MqttConnection connection1, MqttClientException cause) {
                println "mqtt connect error ${cause}"
            }

            @Override
             void onSuccess(MqttConnection connection1) {
                println "mqtt connect success"
                connection.subscribe(Collections.singletonList(new MqttV311TopicAndQosLevel("#", MqttV311QosLevel.AT_MOST_ONCE)), mqttMessageHandler, new MqttSubscribeResultHandler() {
                    @Override
                    void onSuccess(MqttConnection connection2, List<MqttSubscription> subscriptions) {
                        println "mqtt subscribe success"
//                        connection.close();
                    }

                    @Override
                     void onError(MqttConnection connection2, Throwable cause) {
                        println "onError ${cause}"
                    }
                });

            }

            @Override
             void onTimeout(MqttConnection connection1) {
                println "mqtt connect timeout"
            }
        });

        connection.waitForClose()
    }
}
