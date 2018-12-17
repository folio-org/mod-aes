# mod-aes

Copyright (C) 2017-2018 The Open Library Foundation

This software is distributed under the terms of the Apache License,
Version 2.0. See the file "[LICENSE](LICENSE)" for more information.

## Introduction

The mod-aes module provides FOLIO asynchronous event service (AES)

## Permissions

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

Launch mod-aes as described [below](#quick-start).

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

### Quick start

Compile with `mvn clean install`

Run the local stand-alone instance:

```
java -jar target/mod-aes-fat.jar -Dhttp.port=8081 -Dkafka.url=10.23.33.20:9092
```

### API documentation

There is no public API exposed by this module.

### Code analysis

[SonarQube analysis](https://sonarcloud.io/dashboard?id=org.folio%3Amod-aes).

### Download and configuration

The built artifacts for this module are available.
See [configuration](https://dev.folio.org/download/artifacts) for repository access,
and the [Docker image](https://hub.docker.com/r/folioorg/mod-aes/).

