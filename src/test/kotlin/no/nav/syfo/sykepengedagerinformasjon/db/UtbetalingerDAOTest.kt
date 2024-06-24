package no.nav.syfo.sykepengedagerinformasjon.db

import io.kotest.core.spec.style.FunSpec
import io.kotest.core.test.TestCase
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.syfo.sykepengedagerinformasjon.LocalApplication
import no.nav.syfo.sykepengedagerinformasjon.db.util.DatabaseCleaner
import no.nav.syfo.sykepengedagerinformasjon.kafka.consumers.infotrygd.domain.InfotrygdSource
import no.nav.syfo.sykepengedagerinformasjon.kafka.consumers.spleis.domain.UtbetalingSpleis
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.time.LocalDate

@SpringBootTest(classes = [LocalApplication::class])
class UtbetalingerDAOTest : FunSpec() {
    @Autowired
    private lateinit var utbetalingerDAO: UtbetalingerDAO

    @Autowired
    private lateinit var utbetalingSpleisDAO: UtbetalingSpleisDAO

    @Autowired
    private lateinit var utbetalingInfotrygdDAO: UtbetalingInfotrygdDAO

    @Autowired
    private lateinit var databaseCleaner: DatabaseCleaner

    override suspend fun beforeTest(testCase: TestCase) {
        databaseCleaner.clean()
    }

    init {
        extension(SpringExtension)

        test("Selects latest utbetaling Spleis entry") {
            val fnr = "12121212121"
            val forelopigBeregnetSluttPaSykepenger = LocalDate.now().plusDays(15)
            val maxDate = LocalDate.now().plusDays(30)
            val utb =
                UtbetalingSpleis(
                    fødselsnummer = fnr,
                    organisasjonsnummer = "123123",
                    event = "event",
                    type = "type",
                    fom = LocalDate.now().minusDays(3).toString(),
                    tom = LocalDate.now().plusDays(3).toString(),
                    foreløpigBeregnetSluttPåSykepenger = forelopigBeregnetSluttPaSykepenger.toString(),
                    forbrukteSykedager = 4,
                    gjenståendeSykedager = 5,
                    stønadsdager = 5,
                    antallVedtak = 5,
                    utbetalingId = "123456",
                    korrelasjonsId = "654321",
                )
            utbetalingInfotrygdDAO.storeInfotrygdUtbetaling(
                fnr = fnr,
                sykepengerMaxDate = maxDate,
                LocalDate.now().plusDays(20),
                gjenstaendeSykepengedager = 5,
                source = InfotrygdSource.AAP_KAFKA_TOPIC,
            )
            utbetalingSpleisDAO.storeSpleisUtbetaling(utb)

            val result = utbetalingerDAO.fetchMaksDatoByFnr(fnr = fnr)

            result shouldNotBe null
            result?.forelopig_beregnet_slutt shouldBe forelopigBeregnetSluttPaSykepenger
            result?.forelopig_beregnet_slutt shouldNotBe maxDate
        }

        test("Selects latest utbetaling Infotrygd entry") {
            val fnr = "12121212121"
            val forelopigBeregnetSluttPaSykepenger = LocalDate.now().plusDays(15)
            val maxDate = LocalDate.now().plusDays(30)
            val utb =
                UtbetalingSpleis(
                    fødselsnummer = fnr,
                    organisasjonsnummer = "123123",
                    event = "event",
                    type = "type",
                    fom = LocalDate.now().minusDays(3).toString(),
                    tom = LocalDate.now().plusDays(3).toString(),
                    foreløpigBeregnetSluttPåSykepenger = forelopigBeregnetSluttPaSykepenger.toString(),
                    forbrukteSykedager = 4,
                    gjenståendeSykedager = 5,
                    stønadsdager = 5,
                    antallVedtak = 5,
                    utbetalingId = "123456",
                    korrelasjonsId = "654321",
                )

            utbetalingSpleisDAO.storeSpleisUtbetaling(utb)
            utbetalingInfotrygdDAO.storeInfotrygdUtbetaling(
                fnr = fnr,
                sykepengerMaxDate = maxDate,
                LocalDate.now().plusDays(20),
                gjenstaendeSykepengedager = 5,
                source = InfotrygdSource.AAP_KAFKA_TOPIC,
            )

            val result = utbetalingerDAO.fetchMaksDatoByFnr(fnr = fnr)

            result shouldNotBe null
            result?.forelopig_beregnet_slutt shouldNotBe forelopigBeregnetSluttPaSykepenger
            result?.forelopig_beregnet_slutt shouldBe maxDate
        }
    }
}
