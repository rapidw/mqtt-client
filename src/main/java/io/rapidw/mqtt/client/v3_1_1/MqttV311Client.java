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

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.DefaultThreadFactory;

/**
 * MQTT client fully complaints MQTT 3.1.1 specification. Thread safe, should be a singleton.
 */
public class MqttV311Client {
    private EventLoopGroup eventLoopGroup;

    /**
     * constructs a new client
     */
    public MqttV311Client() {
         eventLoopGroup = new NioEventLoopGroup(Runtime.getRuntime().availableProcessors() * 2,
            new DefaultThreadFactory(MqttClientConstants.ThreadNamePrefix, true));
    }

    /**
     * create a new connection
     *
     * @param connectionOption connection options
     * @return created connection, not connected
     */
    public MqttConnection newConnection(MqttConnectionOption connectionOption) {
        Bootstrap bootstrap = new Bootstrap().group(eventLoopGroup).channel(NioSocketChannel.class)
            .option(ChannelOption.SO_REUSEADDR, true)
            .option(ChannelOption.TCP_NODELAY, true);
        if (connectionOption.getTcpConnectTimeout() != 0) {
            bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) connectionOption.getTcpConnectTimeout());
        }
        MqttConnection connection = new MqttConnection(bootstrap, connectionOption);

        bootstrap.handler(new MqttChannelInitializer(connectionOption, connection.handler()));
        return connection;
    }
}
