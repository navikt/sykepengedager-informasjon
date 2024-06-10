package no.nav.syfo.sykepengedagerinformasjon

import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class Application

@Suppress("SpreadOperator") // TODO
fun main(args: Array<String>) {
    val log = LoggerFactory.getLogger(Application::class.java)
    log.info("ASAS")
    runApplication<Application>(*args)
    log.info("QWQW")
}
