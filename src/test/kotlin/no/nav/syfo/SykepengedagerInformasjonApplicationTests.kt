package no.nav.syfo

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(classes = [LocalApplication::class])
class SykepengedagerInformasjonApplicationTests {
    private val log = logger()

    @Test
    fun contextLoads() {
        log.info("Test context loaded")
    }
}
