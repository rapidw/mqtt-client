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

import io.rapidw.mqtt.client.v3_1_1.handler.MqttExceptionHandler;
import io.rapidw.mqtt.codec.v3_1_1.MqttV311Will;

import java.util.Objects;

public class MqttConnectionOption {

    private String host;
    private int port;
    private String username;
    private byte[] password;
    private MqttV311Will will;
    private boolean cleanSession;
    private int keepAliveSeconds;
    private int keepAliveSecondsOffset;

    private String clientId;

    private byte[] serverCertificate;
    private byte[] clientCertificate;
    private int tcpConnectTimeout;
    private int mqttConnectTimeout;


    private MqttExceptionHandler exceptionHandler;

    MqttConnectionOption(String host, int port, String username, byte[] password, MqttV311Will will, boolean cleanSession,
                         int keepAliveSeconds, int keepAliveSecondsOffset, String clientId, byte[] serverCertificate,
                         byte[] clientCertificate, int tcpConnectTimeout, int mqttConnectTimeout, MqttExceptionHandler exceptionHandler) {

        this.host = Objects.requireNonNull(host);
        this.port = port;
        this.username = username;
        this.password = password;
        this.will = will;
        this.cleanSession = cleanSession;
        this.keepAliveSeconds = keepAliveSeconds;
        this.keepAliveSecondsOffset = keepAliveSecondsOffset;
        this.clientId = Objects.requireNonNull(clientId);
        this.serverCertificate = serverCertificate;
        this.clientCertificate = clientCertificate;
        this.tcpConnectTimeout = tcpConnectTimeout;
        this.mqttConnectTimeout = mqttConnectTimeout;
        this.exceptionHandler = Objects.requireNonNull(exceptionHandler);
    }

    public static MqttConnectionOptionBuilder builder() {
        return new MqttConnectionOptionBuilder();
    }


    public String getHost() {
        return this.host;
    }

    public int getPort() {
        return this.port;
    }

    public String getUsername() {
        return this.username;
    }

    public byte[] getPassword() {
        return this.password;
    }

    public MqttV311Will getWill() {
        return this.will;
    }

    public boolean isCleanSession() {
        return this.cleanSession;
    }

    public int getKeepAliveSeconds() {
        return this.keepAliveSeconds;
    }

    public int getKeepAliveSecondsOffset() {
        return this.keepAliveSecondsOffset;
    }


    public String getClientId() {
        return this.clientId;
    }

    public byte[] getServerCertificate() {
        return this.serverCertificate;
    }

    public byte[] getClientCertificate() {
        return this.clientCertificate;
    }

    public int getTcpConnectTimeout() {
        return this.tcpConnectTimeout;
    }

    public int getMqttConnectTimeout() {
        return this.mqttConnectTimeout;
    }


    public MqttExceptionHandler getExceptionHandler() {
        return this.exceptionHandler;
    }

    public static class MqttConnectionOptionBuilder {
        private String host;
        private int port;
        private String username;
        private byte[] password;
        private MqttV311Will will;
        private boolean cleanSession;
        private int keepAliveSeconds;
        private int keepAliveSecondsOffset;
        private String clientId;
        private byte[] serverCertificate;
        private byte[] clientCertificate;
        private int tcpConnectTimeout;
        private int mqttConnectTimeout;
        private MqttExceptionHandler exceptionHandler;

        MqttConnectionOptionBuilder() {
        }

        public MqttConnectionOption.MqttConnectionOptionBuilder host(String host) {
            this.host = Objects.requireNonNull(host);
            return this;
        }

        public MqttConnectionOption.MqttConnectionOptionBuilder port(int port) {
            this.port = port;
            return this;
        }

        public MqttConnectionOption.MqttConnectionOptionBuilder username(String username) {
            this.username = username;
            return this;
        }

        public MqttConnectionOption.MqttConnectionOptionBuilder password(byte[] password) {
            this.password = password;
            return this;
        }

        public MqttConnectionOption.MqttConnectionOptionBuilder will(MqttV311Will will) {
            this.will = will;
            return this;
        }

        public MqttConnectionOption.MqttConnectionOptionBuilder cleanSession(boolean cleanSession) {
            this.cleanSession = cleanSession;
            return this;
        }

        public MqttConnectionOption.MqttConnectionOptionBuilder keepAliveSeconds(int keepAliveSeconds) {
            this.keepAliveSeconds = keepAliveSeconds;
            return this;
        }

        public MqttConnectionOption.MqttConnectionOptionBuilder keepAliveSecondsOffset(int keepAliveSecondsOffset) {
            this.keepAliveSecondsOffset = keepAliveSecondsOffset;
            return this;
        }

        public MqttConnectionOption.MqttConnectionOptionBuilder clientId(String clientId) {
            this.clientId = Objects.requireNonNull(clientId);
            return this;
        }

        /**
         * certificate for verifying server
         * @param serverCertificate X.509 certificate in PEM format
         * @return this
         */
        public MqttConnectionOption.MqttConnectionOptionBuilder serverCertificate(byte[] serverCertificate) {
            this.serverCertificate = serverCertificate;
            return this;
        }

        /**
         * certificate for verifying client
         * @param clientCertificate X.509 certificate in PEM format
         * @return this
         */
        public MqttConnectionOption.MqttConnectionOptionBuilder clientCertificate(byte[] clientCertificate) {
            this.clientCertificate = clientCertificate;
            return this;
        }

        public MqttConnectionOption.MqttConnectionOptionBuilder tcpConnectTimeout(int tcpConnectTimeout) {
            this.tcpConnectTimeout = tcpConnectTimeout;
            return this;
        }

        public MqttConnectionOption.MqttConnectionOptionBuilder mqttConnectTimeout(int mqttConnectTimeout) {
            this.mqttConnectTimeout = mqttConnectTimeout;
            return this;
        }

        /**
         * set global exception handler. When error occurred, if no handler available, global exception handler will be called
         * @param exceptionHandler
         * @return
         */
        public MqttConnectionOption.MqttConnectionOptionBuilder exceptionHandler(MqttExceptionHandler exceptionHandler) {
            this.exceptionHandler = Objects.requireNonNull(exceptionHandler);
            return this;
        }

        public MqttConnectionOption build() {
            return new MqttConnectionOption(host, port, username, password, will, cleanSession, keepAliveSeconds, keepAliveSecondsOffset, clientId, serverCertificate, clientCertificate, tcpConnectTimeout, mqttConnectTimeout, exceptionHandler);
        }

        public String toString() {
            return "MqttConnectionOption.MqttConnectionOptionBuilder(host=" + this.host + ", port=" + this.port + ", username=" + this.username + ", password=" + java.util.Arrays.toString(this.password) + ", will=" + this.will + ", cleanSession=" + this.cleanSession + ", keepAliveSeconds=" + this.keepAliveSeconds + ", keepAliveSecondsOffset=" + this.keepAliveSecondsOffset + ", clientId=" + this.clientId + ", serverCertificate=" + java.util.Arrays.toString(this.serverCertificate) + ", clientCertificate=" + java.util.Arrays.toString(this.clientCertificate) + ", tcpConnectTimeout=" + this.tcpConnectTimeout + ", mqttConnectTimeout=" + this.mqttConnectTimeout + ", exceptionHandler=" + this.exceptionHandler + ")";
        }
    }
}
