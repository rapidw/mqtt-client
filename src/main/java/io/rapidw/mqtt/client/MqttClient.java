package io.rapidw.mqtt.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.DefaultThreadFactory;
import lombok.val;

public class MqttClient {
    private EventLoopGroup eventLoopGroup;

    public MqttClient() {
         eventLoopGroup = new NioEventLoopGroup(Runtime.getRuntime().availableProcessors() * 2,
            new DefaultThreadFactory(MqttClientConstants.ThreadNamePrefix, true));

    }

    public MqttConnection newConnection(MqttConnectionOption connectionOption) {
        val bootstrap = new Bootstrap().group(eventLoopGroup).channel(NioSocketChannel.class)
            .option(ChannelOption.SO_REUSEADDR, true)
            .option(ChannelOption.TCP_NODELAY, true);
        if (connectionOption.getTcpConnectTimeout() != 0) {
            bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectionOption.getTcpConnectTimeout());
        }
        MqttConnection connection = new MqttConnection(bootstrap, connectionOption);

        bootstrap.handler(new MqttChannelInitializer(connectionOption, connection.handler()));
        return connection;
    }
}
