package no.nav.syfo.sykepengedagerinformasjon

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class SykepengedagerInformasjonApplicationTests {
    private val log = logger()

    @Test
    fun contextLoads() {
        log.info("test")
    }
}
