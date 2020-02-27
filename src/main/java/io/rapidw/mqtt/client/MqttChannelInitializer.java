package io.rapidw.mqtt.client;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.rapidw.mqtt.codec.MqttDecoder;
import io.rapidw.mqtt.codec.MqttEncoder;

class MqttChannelInitializer extends ChannelInitializer<NioSocketChannel> {
    private final MqttConnection.Handler handler;
    private MqttConnectionOption connectionOption;

    public MqttChannelInitializer(MqttConnectionOption connectionOption, MqttConnection.Handler handler) {
        this.connectionOption = connectionOption;
        this.handler = handler;
    }

    @Override
    protected void initChannel(NioSocketChannel channel) throws Exception {
        ChannelPipeline pipeline = channel.pipeline();
        if (connectionOption.getSslCertificate() != null) {
            SslContext sslContext = SslContextBuilder.forClient().trustManager(connectionOption.getSslCertificate()).build();
            pipeline.addLast(sslContext.newHandler(channel.alloc(), connectionOption.getHost(), connectionOption.getPort()));
        }
        pipeline.addLast(new MqttDecoder());
        pipeline.addLast(MqttEncoder.INSTANCE);
        pipeline.addLast(MqttClientConstants.MQTT_HANDLER, handler);
    }
}
