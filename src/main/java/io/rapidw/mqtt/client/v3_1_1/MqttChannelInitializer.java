/**
 * Copyright 2023 Rapidw
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

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.rapidw.mqtt.codec.v3_1_1.MqttV311Decoder;
import io.rapidw.mqtt.codec.v3_1_1.MqttV311Encoder;

import javax.net.ssl.TrustManagerFactory;
import java.io.ByteArrayInputStream;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

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
        byte[] sslCertificateBytes = connectionOption.getServerCertificate();
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
        pipeline.addLast(new MqttV311Decoder());
        pipeline.addLast(MqttV311Encoder.INSTANCE);
        pipeline.addLast(MqttClientConstants.MQTT_CLIENT_HANDLER_NAME, handler);
    }
}
