# sykepengedager-informasjon

Core functions of this application:

1. Listens to topics: Monitors the **team-esyfo.sykepengedager.infotrygd.v1** and **tbd.utbetaling** topics, capturing payments data for storage in the database.
2. Serves data via REST endpoints: Provides access to the maximum date and other relevant data like "gjenstaende_sykedager" or creation date through RESTful API endpoints.
3. Writes data to topic: Publishes the maximum date and other relevant information like "gjenstaende_sykedager" or creation date to the **'sykepengedager-informasjon-topic'** topic.

## Access to the topic
To get access to the topic, please reach team  `#esyfo` on Slack about adding application with reading rights to the topic and add `sykepengedager-informasjon` to the outbound rules in your app.

### Topic's DTO
````
data class KSykepengedagerInformasjonDTO(
    val id: String,
    val personIdent: String,
    val forelopigBeregnetSlutt: LocalDate,
    val utbetaltTom: LocalDate,
    val gjenstaendeSykedager: String,
    val createdAt: LocalDateTime,
)
````

## Technologies used

* Docker
* Gradle
* Kafka
* Kotlin
* Spring Boot
* Postgres


### Test
Run test: `./gradlew  test`


----

## Blueprint for creating a topic to consume data from Infotrygd
The process looks roughly like this (the person who worked wit this before we made our own topic: [Anders Østby](https://nav-it.slack.com/archives/D02BAT7JXRB)(not in our team eSyfo)):
+ Infotrygd is not in NAIS, so for it to write to our topic, we need to create an AivenApplication as described  [here](https://docs.nais.io/persistence/kafka/how-to/access-from-non-nais/)


+ Create a regular Kafka topic.


+ Fetch the generated credentials and share them in a Jira request to Infotrygd. See [the previous Jira request](https://jira.adeo.no/browse/PK-61174) for reference.


+ Commands to create the topic and retrieve credentials ("ASDF" and "XYZ" are DNS labels defined for the secret in the NAIS aiven config in the [application](https://github.com/navikt/sykepengedager-informasjon/blob/main/nais/aiven-sykepengedager-infotrygd-dev.yml):

    +  DEV
    ```shell
    kubectl get secret aiven-sykepengedager-infotrygd-dev-XYZ -ojson | jq -r ".data[\"client.keystore.p12\"]" | base64 -d > ./infotrygd_secrets/sykepengedager-infotrygd-dev-keystore.p12
    kubectl get secret aiven-sykepengedager-infotrygd-dev-XYZ -ojson | jq -r ".data[\"client.truststore.jks\"]" | base64 -d > ./infotrygd_secrets/sykepengedager-infotrygd-dev-truststore.jks
    ```

    +  PROD
    ```shell
    kubectl get secret aiven-sykepengedager-infotrygd-prod-ASDF -ojson | jq -r ".data[\"client.keystore.p12\"]" | base64 -d > ./infotrygd_secrets/sykepengedager-infotrygd-prod-keystore.p12
    kubectl get secret aiven-sykepengedager-infotrygd-prod-ASDF -ojson | jq -r ".data[\"client.truststore.jks\"]" | base64 -d > ./infotrygd_secrets/sykepengedager-infotrygd-prod-truststore.jks
    ```
----

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
