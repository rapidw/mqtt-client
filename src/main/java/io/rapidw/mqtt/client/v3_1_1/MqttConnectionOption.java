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
import java.util.concurrent.TimeUnit;

public class MqttConnectionOption {

    private final String host;
    private final int port;
    private final String username;
    private final byte[] password;
    private final MqttV311Will will;
    private final boolean cleanSession;
    private final long keepAliveSeconds;

    private final String clientId;

    private final byte[] serverCertificate;
    private final byte[] clientCertificate;
    private final long tcpConnectTimeout;
    private final long mqttConnectTimeout;


    private final MqttExceptionHandler exceptionHandler;

    MqttConnectionOption(String host, int port, String username, byte[] password, MqttV311Will will, boolean cleanSession,
                         int keepAlive, TimeUnit keepAliveTimeUnit, String clientId, byte[] serverCertificate, byte[] clientCertificate,
                         int tcpConnectTimeout, TimeUnit tcpConnectTimeoutTimeUnit, int mqttConnectTimeout, TimeUnit mqttConnectTimeoutTimeUnit,
                         MqttExceptionHandler exceptionHandler) {

        this.host = Objects.requireNonNull(host);
        this.port = port;
        this.username = username;
        this.password = password;
        this.will = will;
        this.cleanSession = cleanSession;
        if (keepAliveTimeUnit != null) {
            this.keepAliveSeconds = TimeUnit.SECONDS.convert(keepAlive, keepAliveTimeUnit);
        } else {
            throw new MqttClientException("keepAliveTimeUnit required");
        }
        if (keepAlive < 0) {
            throw new MqttClientException("invalid keepAlive");
        }
        this.clientId = Objects.requireNonNull(clientId);
        this.serverCertificate = serverCertificate;
        this.clientCertificate = clientCertificate;
        if (tcpConnectTimeout < 0) {
            throw new MqttClientException("invalid tcpConnectTimeout");
        }
        if (tcpConnectTimeoutTimeUnit != null) {
            this.tcpConnectTimeout = TimeUnit.MILLISECONDS.convert(tcpConnectTimeout, tcpConnectTimeoutTimeUnit);
        } else {
            throw new MqttClientException("tcpConnectTimeoutTimeUnit required");
        }
        if (mqttConnectTimeout < 0) {
            throw new MqttClientException("invalid mqttConnectTimeout");
        }
        if (mqttConnectTimeoutTimeUnit != null) {
            this.mqttConnectTimeout = TimeUnit.MILLISECONDS.convert(mqttConnectTimeout, mqttConnectTimeoutTimeUnit);
        } else {
            throw new MqttClientException("mqttConnectTimeoutTimeUnit required");
        }
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

    public long getKeepAliveSeconds() {
        return this.keepAliveSeconds;
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

    public long getTcpConnectTimeout() {
        return this.tcpConnectTimeout;
    }

    public long getMqttConnectTimeout() {
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
        private int keepAlive;
        private TimeUnit keepAliveTimeUnit;
        private String clientId;
        private byte[] serverCertificate;
        private byte[] clientCertificate;
        private int tcpConnectTimeout;
        private TimeUnit tcpConnectTimeoutTimeUnit;
        private int mqttConnectTimeout;
        private TimeUnit mqttConnectTimeoutTimeUnit;
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

        /**
         * set keepAlive for this connection.
         * @param keepAlive keepAlive in CONNECT packet. 0 for close automatic heartbeat
         * @return this
         */
        public MqttConnectionOption.MqttConnectionOptionBuilder keepAlive(int keepAlive) {
            this.keepAlive = keepAlive;
            return this;
        }

        /**
         * set time unit of keepAlive
         * @param keepAliveTimeUnit time unit of keepAlive
         * @return this
         */
        public MqttConnectionOption.MqttConnectionOptionBuilder keepAliveTimeUnit(TimeUnit keepAliveTimeUnit) {
            this.keepAliveTimeUnit = keepAliveTimeUnit;
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

        public MqttConnectionOption.MqttConnectionOptionBuilder tcpConnectTimeoutTimeUnit(TimeUnit tcpConnectTimeoutTimeUnit) {
            this.tcpConnectTimeoutTimeUnit = tcpConnectTimeoutTimeUnit;
            return this;
        }

        public MqttConnectionOption.MqttConnectionOptionBuilder mqttConnectTimeout(int mqttConnectTimeout) {
            this.mqttConnectTimeout = mqttConnectTimeout;
            return this;
        }

        public MqttConnectionOption.MqttConnectionOptionBuilder mqttConnectTimeoutTimeUnit(TimeUnit mqttConnectTimeoutTimeUnit) {
            this.mqttConnectTimeoutTimeUnit = mqttConnectTimeoutTimeUnit;
            return this;
        }

        /**
         * set global exception handler. When error occurred, if no handler available, global exception handler will be called
         * @param exceptionHandler exception handler
         * @return this
         */
        public MqttConnectionOption.MqttConnectionOptionBuilder exceptionHandler(MqttExceptionHandler exceptionHandler) {
            this.exceptionHandler = Objects.requireNonNull(exceptionHandler);
            return this;
        }

        public MqttConnectionOption build() {
            return new MqttConnectionOption(host, port, username, password, will, cleanSession, keepAlive, keepAliveTimeUnit,
                clientId, serverCertificate, clientCertificate, tcpConnectTimeout,
                tcpConnectTimeoutTimeUnit, mqttConnectTimeout, mqttConnectTimeoutTimeUnit, exceptionHandler);
        }
    }
}
