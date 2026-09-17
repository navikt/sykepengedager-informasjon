# sykepengedager-informasjon

- `./gradlew test` runs tests. `./gradlew build` also runs ktlint and
  `verifyCoroutineRuntime`, which checks the packaged coroutine ABI.
- Preserve the runtime coroutine dependencies: compilation/test success alone
  does not prove that Spring suspend endpoints work in the packaged jar.
- Database tests use Zonky embedded PostgreSQL rather than Testcontainers.
- TokenX APIs derive the person from the token and enforce high-assurance
  `acr` and client checks. Azure AD APIs require `Nav-Personident`, `Nav-Call-Id`
  and an explicit veileder access check for the person.
- Infotrygd and Spleis records feed separate tables. The Kafka publication and
  REST API select different records: Kafka uses `MAXDATO` ordered by creation;
  REST combines `MAXDATO` and `UTBETALING` with payment-date ordering. Preserve
  both behaviours when changing maksdato or remaining-day calculations.
- Payment history and person identifiers must stay out of ordinary logs and
  metric labels.
