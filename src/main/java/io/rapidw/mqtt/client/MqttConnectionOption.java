package io.rapidw.mqtt.client;

import io.rapidw.mqtt.client.handler.MqttExceptionHandler;
import io.rapidw.mqtt.codec.MqttWill;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;

import java.io.InputStream;

@Getter
@Builder
public class MqttConnectionOption {
    @NonNull
    private String host;
    private int port;
    private String username;
    private byte[] password;
    private MqttWill will;
    private boolean cleanSession;
    private int keepAliveSeconds;
    private int keepAliveSecondsOffset;
    @NonNull
    private String clientId;

    private byte[] sslCertificate;
    private int tcpConnectTimeout;
    private int mqttConnectTimeout;

    @NonNull
    private MqttExceptionHandler exceptionHandler;

//    public MqttConnectionOption(String host, int port, String username, byte[] password, MqttWill will, boolean cleanSession,
//                                int keepAliveSeconds, int keepAliveSecondsOffset, String clientId, InputStream sslCertificate,
//                                int tcpConnectTimeout, int mqttConnectTimeout, MqttExceptionHandler exceptionHandler) {
//
//    }
}
