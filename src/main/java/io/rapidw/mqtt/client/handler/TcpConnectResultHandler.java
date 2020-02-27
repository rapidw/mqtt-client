package io.rapidw.mqtt.client.handler;

public interface TcpConnectResultHandler {
    void onSuccess();
    void onError(Throwable throwable);
    void onTimeout();
}
