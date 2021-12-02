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
package io.rapidw.mqtt.client.v3_1_1;

import io.rapidw.mqtt.codec.v3_1_1.MqttV311ConnectReturnCode;

/**
 * exception contains a MQTT connect return code, only will be threw during establishing MQTT connection
 */
public class MqttConnectionException extends MqttClientException {

    private final MqttV311ConnectReturnCode connectReturnCode;

    MqttConnectionException(MqttV311ConnectReturnCode connectReturnCode) {
        super("connect failed, return code is " + connectReturnCode.name());
        this.connectReturnCode = connectReturnCode;
    }

    /**
     * get MQTT connect return code
     * @return connect return code
     */
    public MqttV311ConnectReturnCode getConnectReturnCode() {
        return connectReturnCode;
    }
}
