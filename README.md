# mod-aes

Copyright (C) 2017-2018 The Open Library Foundation

This software is distributed under the terms of the Apache License,
Version 2.0. See the file "[LICENSE](LICENSE)" for more information.

## Introduction

The mod-aes module provides FOLIO asynchronous event service (AES)

## Permissions

## Additional information

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

[SonarQube analysis](https://sonarcloud.io/dashboard?id=org.folio.rest%3Amod-aes).

### Download and configuration

The built artifacts for this module are available.
See [configuration](https://dev.folio.org/download/artifacts) for repository access,
and the [Docker image](https://hub.docker.com/r/folioorg/mod-aes/).

