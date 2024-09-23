# sykepengedager-informasjon

Core functions of this application:

1. Listens to topics: Monitors the **aap.sykepengedager.infotrygd.v1** and **tbd.utbetaling** topics, capturing payments data for storage in the database.
2. Serves data via REST endpoints: Provides access to the maximum date and other relevant data like "gjenstaende_sykedager" or creation date through RESTful API endpoints.
3. Writes data to topic: Publishes the maximum date and other relevant information like "gjenstaende_sykedager" or creation date to the **'sykepengedager-informasjon-topic'** topic.

## Technologies used

* Docker
* Gradle
* Kafka
* Kotlin
* Spring Boot
* Postgres


### Test
Run test: `./gradlew  test`

### Reference Documentation
For further reference, please consider the following sections:

* [Official Gradle documentation](https://docs.gradle.org)
* [Spring Boot Gradle Plugin Reference Guide](https://docs.spring.io/spring-boot/docs/3.3.0/gradle-plugin/reference/html/)
* [Create an OCI image](https://docs.spring.io/spring-boot/docs/3.3.0/gradle-plugin/reference/html/#build-image)
* [Spring Boot DevTools](https://docs.spring.io/spring-boot/docs/3.3.0/reference/htmlsingle/index.html#using.devtools)
* [Flyway Migration](https://docs.spring.io/spring-boot/docs/3.3.0/reference/htmlsingle/index.html#howto.data-initialization.migration-tool.flyway)
* [Spring for Apache Kafka](https://docs.spring.io/spring-boot/docs/3.3.0/reference/htmlsingle/index.html#messaging.kafka)
* [Prometheus](https://docs.spring.io/spring-boot/docs/3.3.0/reference/htmlsingle/index.html#actuator.metrics.export.prometheus)


## Contact

### For NAV employees

We are available at the Slack channel `#esyfo`
