package no.nav.syfo.db

import io.kotest.core.spec.style.FunSpec
import io.kotest.core.test.TestCase
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.syfo.sykepengedagerinformasjon.LocalApplication
import no.nav.syfo.sykepengedagerinformasjon.db.util.DatabaseCleaner
import no.nav.syfo.sykepengedagerinformasjon.kafka.consumers.spleis.domain.UtbetalingSpleis
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.time.LocalDate

@SpringBootTest(classes = [LocalApplication::class])
class UtbetalingSpleisDAOTest : FunSpec() {
    @Autowired
    private lateinit var utbetalingSpleisDAO: UtbetalingSpleisDAO

    @Autowired
    private lateinit var databaseCleaner: DatabaseCleaner

    override suspend fun beforeTest(testCase: TestCase) {
        databaseCleaner.clean()
    }

    init {
        extension(SpringExtension)

        val fnr = "12121212121"
        val utbetalingId = "123456"
        val utb =
            UtbetalingSpleis(
                fødselsnummer = fnr,
                organisasjonsnummer = "123123",
                event = "event",
                type = "type",
                fom = LocalDate.now().minusDays(3).toString(),
                tom = LocalDate.now().plusDays(3).toString(),
                foreløpigBeregnetSluttPåSykepenger = LocalDate.now().toString(),
                forbrukteSykedager = 4,
                gjenståendeSykedager = 5,
                stønadsdager = 5,
                antallVedtak = 5,
                utbetalingId = utbetalingId,
                korrelasjonsId = "654321",
            )

        test("Store utbetaling Spleis") {
            utbetalingSpleisDAO.storeSpleisUtbetaling(utb)

            val result =
                utbetalingSpleisDAO.fetchSpleisUtbettalingByFnr(
                    fnr = fnr,
                )
            result shouldNotBe null
            result?.fødselsnummer shouldBe fnr
            result?.utbetalingId shouldBe utbetalingId
        }
    }
}
