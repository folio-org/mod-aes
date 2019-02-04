# mod-aes

Copyright (C) 2017-2019 The Open Library Foundation

This software is distributed under the terms of the Apache License,
Version 2.0. See the file "[LICENSE](LICENSE)" for more information.

## Introduction

The mod-aes module implements FOLIO asynchronous event service (AES). Currently, it acts as a filter to capture traffic as JSON messages. It then routes the messages to different Kafka topic according to JSON path based routing rules that are stored in mod-config.

### Quick start

Compile with `mvn clean install`

Run local stand-alone instance with connection to Kafaka as below. If Kafka connection option is not provided, the module will fall back to output messages in the log.

```
java -jar target/mod-aes-fat.jar -Dhttp.port=8081 -Dkafka.url=10.23.33.20:9092
```
## Sample routing rule

Insert rules like below to mod-config. If no rules exist in mod-config, the module will fall back to route all messages to tenant_default topic.
```
{
  "module": "AES",
  "configName": "routing_rules",
  "code": "rule_1",
  "description": "for testing",
  "default": true,
  "enabled": true,
  "value": "{\"criteria\":\"$[?(@.path =~ /.login.*/i)]\",\"target\":\"login\"}"
}
```
## Permissions

It is a pure backend module and there are no specific module permissions defined for now.

## Additional information

### Messaging Queue

The reference implementation will use [Apache Kafka](https://kafka.apache.org/)
as the messaging queue and [Apache Zookeeper](https://zookeeper.apache.org/)
as the coordinator.

To set this up we recommend using docker images as follows:

`$ git clone https://github.com/wurstmeister/kafka-docker.git`

Edit the docker-compose.yml file as follows:
```$ cat docker-compose.yml
version: '2'
services:
  zookeeper:
    image: zookeeper
    restart: always
    hostname: zoo1
    ports:
      - 2181:2181
    environment:
      ZOO_MY_ID: 1
      ZOO_SERVERS: server.1=<ZOOKEEPER_IP>:2888:3888
  kafka:
    build: .
    ports:
      - 9092:9092
    environment:
      KAFKA_ADVERTISED_HOST_NAME: <KAFKA_IP>
      KAFKA_ZOOKEEPER_CONNECT: <ZOOKEEPER_IP>:2181
    volumes:
      - /var/run/docker.sock:/var/run/docker.sock
```

If running on the same machine the `KAFKA_IP` and `ZOOKEEPER_IP` will be the same.

Execute the container as follows:
- Start: `$ /usr/local/bin/docker-compose up -d`
- Stop: `$ /usr/local/bin/docker-compose stop`

Note: you may need to be root (sudo) to do this.

Launch mod-aes as described [above](#quick-start).

Example AES input:
```
$ curl -X POST http://localhost:8081/test -H 'Content-Type: application/json'   -H 'X-Okapi-Tenant: test' -H 'x-okapi-filter: pre'  -d '{"test": "some value"}'
```

### Other documentation

Other [modules](https://dev.folio.org/source-code/#server-side) are described,
with further FOLIO Developer documentation at [dev.folio.org](https://dev.folio.org/)

### Issue tracker

See project [MODAES](https://issues.folio.org/browse/MODAES)
at the [FOLIO issue tracker](https://dev.folio.org/guidelines/issue-tracker).

### API documentation

There is no public API exposed by this module.

### Code analysis

[SonarQube analysis](https://sonarcloud.io/dashboard?id=org.folio%3Amod-aes).

### Download and configuration

The built artifacts for this module are available.
See [configuration](https://dev.folio.org/download/artifacts) for repository access,
and the [Docker image](https://hub.docker.com/r/folioorg/mod-aes/).

