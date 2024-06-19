package no.nav.syfo.sykepengedagerinformasjon

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class LocalApplication

fun main(args: Array<String>) {
    runApplication<LocalApplication>(*args)
}
