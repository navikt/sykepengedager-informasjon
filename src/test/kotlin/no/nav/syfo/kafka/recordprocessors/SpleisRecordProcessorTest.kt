package no.nav.syfo.kafka.recordprocessors

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.core.test.TestCase
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.syfo.LocalApplication
import no.nav.syfo.db.UtbetalingSpleisDAO
import no.nav.syfo.db.util.DatabaseCleaner
import no.nav.syfo.kafka.consumers.spleis.domain.UTBETALING_UTBETALT
import no.nav.syfo.kafka.consumers.spleis.domain.UtbetalingSpleis
import no.nav.syfo.kafka.producers.SykepengedagerInformasjonKafkaService
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import java.time.LocalDate

@SpringBootTest(classes = [LocalApplication::class, SpleisRecordProcessorTest.TestConfig::class])
class SpleisRecordProcessorTest : FunSpec() {

    @Autowired
    private lateinit var spleisRecordProcessor: SpleisRecordProcessor

    @Autowired
    private lateinit var utbetalingSpleisDAO: UtbetalingSpleisDAO

    @Autowired
    private lateinit var databaseCleaner: DatabaseCleaner

    @Autowired
    private lateinit var mockedKafkaService: SykepengedagerInformasjonKafkaService

    override suspend fun beforeTest(testCase: TestCase) {
        databaseCleaner.clean()
        clearMocks(mockedKafkaService)
    }

    init {
        extension(SpringExtension)

        test("Stores UtbetalingSpleis when publish succeeds") {
            val fnr = "10101010101"
            val utbetaling = utbetalingTemplate(
                fnr = fnr,
                utbetalingId = "happy-1",
            )
            val json = jacksonObjectMapper().writeValueAsString(utbetaling)
            val record = ConsumerRecord("spleis-topic", 0, 0L, fnr, json)

            spleisRecordProcessor.processRecord(record)

            val stored = utbetalingSpleisDAO.fetchSpleisUtbettalingByFnr(fnr)
            stored shouldNotBe null
            stored?.utbetalingId shouldBe "happy-1"
            verify(exactly = 1) { mockedKafkaService.publishSykepengedagerInformasjonEvent(fnr) }
        }

        test("Does not store UtbetalingSpleis when publish throws exception (transaction rollback)") {
            val fnr = "12121212121"
            every {
                mockedKafkaService.publishSykepengedagerInformasjonEvent(fnr)
            } throws RuntimeException("Publish failed")

            val utbetaling = utbetalingTemplate(
                fnr = fnr,
                utbetalingId = "fail-1",
            )

            val json = jacksonObjectMapper().writeValueAsString(utbetaling)
            val record = ConsumerRecord("spleis-topic", 0, 0L, fnr, json)

            shouldThrow<RuntimeException> {
                spleisRecordProcessor.processRecord(record)
            }

            val stored = utbetalingSpleisDAO.fetchSpleisUtbettalingByFnr(fnr)
            stored shouldBe null
            verify(exactly = 1) { mockedKafkaService.publishSykepengedagerInformasjonEvent(fnr) }
        }
    }

    private fun utbetalingTemplate(
        fnr: String,
        utbetalingId: String,
    ) = UtbetalingSpleis(
        fødselsnummer = fnr,
        organisasjonsnummer = "123456789",
        event = UTBETALING_UTBETALT,
        type = "UTBETALING",
        foreløpigBeregnetSluttPåSykepenger = LocalDate.now().plusDays(20).toString(),
        forbrukteSykedager = 10,
        gjenståendeSykedager = 200,
        stønadsdager = 250,
        antallVedtak = 3,
        fom = LocalDate.now().minusDays(5).toString(),
        tom = LocalDate.now().toString(),
        utbetalingId = utbetalingId,
        korrelasjonsId = "corr-$utbetalingId",
    )

    @TestConfiguration
    class TestConfig {
        @Bean
        @Primary
        fun mockedSykepengedagerInformasjonKafkaService(): SykepengedagerInformasjonKafkaService = mockk(relaxed = true)
    }
}
