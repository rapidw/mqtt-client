# Rapidw MQTT client

[![Build Status](https://travis-ci.org/rapidw/mqtt-client.svg?branch=master)](https://travis-ci.org/rapidw/mqtt-client)
[![Maven Central](http://img.shields.io/maven-central/v/io.rapidw.mqtt/rapidw-mqtt-client)](https://search.maven.org/artifact/io.rapidw.mqtt/rapidw-mqtt-client)
[![Bintray](http://img.shields.io/bintray/v/pvtyuan/maven/rapidw-mqtt-client)](https://bintray.com/pvtyuan/maven/rapidw-mqtt-client/_latestVersion)
[![License](https://img.shields.io/github/license/rapidw/mqtt-client)](https://github.com/rapidw/mqtt-client/blob/master/LICENSE)

a fully asynchronous client for MQTT 3.1.1 based on [rapidw MQTT Codec](https://github.com/rapidw/mqtt-codec) 
## Features

- [x] CONNECT/CONACK
- [x] SUBSCRIBE/SUBACK
    * [x] QoS 0
    * [ ] QoS 1
    * [ ] QoS 2
- [x] UNSUBSCRIBE/UNSUBACK
- [x] PUBLISH
    * [x] QoS 0
    * [ ] QoS 1
    * [ ] QoS 2    
- [x] automatic PINGREQ/PINGRESP
- [x] DISCONNECT
