package io.rapidw.mqtt.client;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.rapidw.mqtt.codec.MqttDecoder;
import io.rapidw.mqtt.codec.MqttEncoder;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import javax.net.ssl.TrustManagerFactory;
import java.io.ByteArrayInputStream;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

@Slf4j
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
        val sslCertificateBytes = connectionOption.getSslCertificate();
        if (sslCertificateBytes != null) {
            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
            X509Certificate cert = (X509Certificate) certificateFactory.generateCertificate(new ByteArrayInputStream(sslCertificateBytes));
            String alias = cert.getSubjectX500Principal().getName();

            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(null, null);
            keyStore.setCertificateEntry(alias, cert);

            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(keyStore);

            SslContext sslContext = SslContextBuilder.forClient().trustManager(tmf).startTls(false).build();
            pipeline.addFirst(sslContext.newHandler(channel.alloc(), connectionOption.getHost(), connectionOption.getPort()));
        }
        pipeline.addLast(new MqttDecoder());
        pipeline.addLast(MqttEncoder.INSTANCE);
        pipeline.addLast(MqttClientConstants.MQTT_HANDLER, handler);
    }
}
