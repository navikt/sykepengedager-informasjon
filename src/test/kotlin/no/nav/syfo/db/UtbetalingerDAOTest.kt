package no.nav.syfo.db

import com.ninjasquad.springmockk.MockkBean
import io.kotest.core.spec.style.FunSpec
import io.kotest.core.test.TestCase
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.syfo.LocalApplication
import no.nav.syfo.config.EmbeddedPostgresTestConfig
import no.nav.syfo.db.util.DatabaseCleaner
import no.nav.syfo.kafka.consumers.aapInfotrygd.domain.InfotrygdSource
import no.nav.syfo.kafka.consumers.spleis.domain.DagType
import no.nav.syfo.kafka.consumers.spleis.domain.UtbetalingSpleis
import no.nav.syfo.kafka.consumers.spleis.domain.UtbetalingsdagDto
import no.nav.syfo.kafka.producers.SykepengedagerInformasjonKafkaService
import no.nav.syfo.kafka.recordprocessors.SpleisRecordProcessor
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.time.LocalDate

@SpringBootTest(classes = [LocalApplication::class, EmbeddedPostgresTestConfig::class])
class UtbetalingerDAOTest : FunSpec() {
    @Autowired
    private lateinit var utbetalingerDAO: UtbetalingerDAO

    @Autowired
    private lateinit var utbetalingSpleisDAO: UtbetalingSpleisDAO

    @Autowired
    private lateinit var utbetalingInfotrygdDAO: UtbetalingInfotrygdDAO

    @Autowired
    private lateinit var spleisRecordProcessor: SpleisRecordProcessor

    @MockkBean(relaxed = true)
    private lateinit var sykepengedagerInformasjonKafkaService: SykepengedagerInformasjonKafkaService

    @Autowired
    private lateinit var databaseCleaner: DatabaseCleaner

    override suspend fun beforeTest(testCase: TestCase) {
        databaseCleaner.clean()
        // just to make detekt happy
        sykepengedagerInformasjonKafkaService.log.info("started")
        utbetalingSpleisDAO.fetchSpleisUtbetalingByFnr("")
    }

    init {
        extension(SpringExtension)

        test("Selects latest utbetaling Spleis entry") {
            val fnr = "12121212121"
            val forelopigBeregnetSluttPaSykepenger = LocalDate.now().plusDays(15)
            val maxDate = LocalDate.now().plusDays(30)
            val fom = LocalDate.now().minusDays(3)
            val tom = LocalDate.now().plusDays(20)
            val utb =
                UtbetalingSpleis(
                    fødselsnummer = fnr,
                    organisasjonsnummer = "123123",
                    event = "event",
                    type = "type",
                    fom = fom.toString(),
                    tom = tom.toString(),
                    foreløpigBeregnetSluttPåSykepenger = forelopigBeregnetSluttPaSykepenger.toString(),
                    forbrukteSykedager = 4,
                    gjenståendeSykedager = 5,
                    stønadsdager = 5,
                    antallVedtak = 5,
                    utbetalingId = "123456",
                    korrelasjonsId = "654321",
                    utbetalingsdager = createUtbetalingsdager(fom, tom),
                )
            utbetalingInfotrygdDAO.storeInfotrygdUtbetaling(
                fnr = fnr,
                sykepengerMaxDate = maxDate,
                LocalDate.now().plusDays(20),
                gjenstaendeSykepengedager = 5,
                source = InfotrygdSource.AAP_KAFKA_TOPIC,
            )
            spleisRecordProcessor.processUtbetalingSpleisEvent(utb)

            val result = utbetalingerDAO.fetchMaksDatoByFnr(fnr = fnr)

            result shouldNotBe null
            result?.forelopig_beregnet_slutt shouldBe forelopigBeregnetSluttPaSykepenger
            result?.forelopig_beregnet_slutt shouldNotBe maxDate
        }

        test("Selects latest utbetaling Infotrygd entry") {
            val fnr = "12121212121"
            val forelopigBeregnetSluttPaSykepenger = LocalDate.now().plusDays(15)
            val maxDate = LocalDate.now().plusDays(30)
            val fom = LocalDate.now().minusDays(3)
            val tom = LocalDate.now().plusDays(3)
            val utb =
                UtbetalingSpleis(
                    fødselsnummer = fnr,
                    organisasjonsnummer = "123123",
                    event = "event",
                    type = "type",
                    fom = fom.toString(),
                    tom = tom.toString(),
                    foreløpigBeregnetSluttPåSykepenger = forelopigBeregnetSluttPaSykepenger.toString(),
                    forbrukteSykedager = 4,
                    gjenståendeSykedager = 5,
                    stønadsdager = 5,
                    antallVedtak = 5,
                    utbetalingId = "123456",
                    korrelasjonsId = "654321",
                    utbetalingsdager = createUtbetalingsdager(fom, tom),
                )

            spleisRecordProcessor.processUtbetalingSpleisEvent(utb)
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

        test("Selects latest utbetaling by utebetalt_tom") {
            val fnr = "12121212121"
            val forelopigBeregnetSluttPaSykepenger = LocalDate.now().plusDays(15)
            val forelopigBeregnetSluttPaSykepengerLengst = LocalDate.now().plusDays(15)
            val maxDate = LocalDate.now().plusDays(30)
            val fom = LocalDate.now().minusDays(3)
            val tom = LocalDate.now().plusDays(4)
            val utb =
                UtbetalingSpleis(
                    fødselsnummer = fnr,
                    organisasjonsnummer = "123123",
                    event = "event",
                    type = "type",
                    fom = fom.toString(),
                    tom = tom.toString(),
                    foreløpigBeregnetSluttPåSykepenger = forelopigBeregnetSluttPaSykepenger.toString(),
                    forbrukteSykedager = 4,
                    gjenståendeSykedager = 5,
                    stønadsdager = 5,
                    antallVedtak = 5,
                    utbetalingId = "223456",
                    korrelasjonsId = "654321",
                    utbetalingsdager = createUtbetalingsdager(fom, tom),
                )
            val utb2 =
                UtbetalingSpleis(
                    fødselsnummer = fnr,
                    organisasjonsnummer = "123123",
                    event = "event",
                    type = "type",
                    fom = fom.toString(),
                    tom = tom.plusDays(1).toString(),
                    foreløpigBeregnetSluttPåSykepenger = forelopigBeregnetSluttPaSykepengerLengst.toString(),
                    forbrukteSykedager = 4,
                    gjenståendeSykedager = 5,
                    stønadsdager = 5,
                    antallVedtak = 5,
                    utbetalingId = "123456",
                    korrelasjonsId = "654321",
                    utbetalingsdager = createUtbetalingsdager(fom, tom.plusDays(1)),
                )

            spleisRecordProcessor.processUtbetalingSpleisEvent(utb)
            spleisRecordProcessor.processUtbetalingSpleisEvent(utb2)
            utbetalingInfotrygdDAO.storeInfotrygdUtbetaling(
                fnr = fnr,
                sykepengerMaxDate = maxDate,
                LocalDate.now().plusDays(3),
                gjenstaendeSykepengedager = 5,
                source = InfotrygdSource.AAP_KAFKA_TOPIC,
            )

            val result = utbetalingerDAO.fetchMaksDatoByFnr(fnr = fnr)

            result shouldNotBe null
            result?.utbetalt_tom shouldBe LocalDate.now().plusDays(5)
            result?.forelopig_beregnet_slutt shouldBe forelopigBeregnetSluttPaSykepengerLengst
        }
        test("Selects latest utbetaling by utebetalt_tom when soknad ikke godkjent") {
            val fnr = "12121212121"
            val forelopigBeregnetSluttPaSykepenger = LocalDate.now().plusDays(15)
            val forelopigBeregnetSluttPaSykepengerLengst = LocalDate.now().plusDays(15)
            val fom = LocalDate.now().minusDays(3)
            val tom = LocalDate.now().plusDays(4)
            val utb =
                UtbetalingSpleis(
                    fødselsnummer = fnr,
                    organisasjonsnummer = "123123",
                    event = "event",
                    type = "type",
                    fom = fom.toString(),
                    tom = tom.plusDays(1).toString(),
                    foreløpigBeregnetSluttPåSykepenger = forelopigBeregnetSluttPaSykepengerLengst.toString(),
                    forbrukteSykedager = 4,
                    gjenståendeSykedager = 5,
                    stønadsdager = 5,
                    antallVedtak = 5,
                    utbetalingId = "123456",
                    korrelasjonsId = "654321",
                    utbetalingsdager = createUtbetalingsdager(fom, tom.plusDays(1), DagType.AvvistDag),
                )
            val utb2 =
                UtbetalingSpleis(
                    fødselsnummer = fnr,
                    organisasjonsnummer = "123123",
                    event = "event",
                    type = "type",
                    fom = fom.toString(),
                    tom = tom.toString(),
                    foreløpigBeregnetSluttPåSykepenger = forelopigBeregnetSluttPaSykepenger.toString(),
                    forbrukteSykedager = 4,
                    gjenståendeSykedager = 5,
                    stønadsdager = 5,
                    antallVedtak = 5,
                    utbetalingId = "223456",
                    korrelasjonsId = "654321",
                    utbetalingsdager = createUtbetalingsdager(fom, tom),
                )

            spleisRecordProcessor.processUtbetalingSpleisEvent(utb)
            spleisRecordProcessor.processUtbetalingSpleisEvent(utb2)
            val result = utbetalingerDAO.fetchMaksDatoByFnr(fnr = fnr)

            result shouldNotBe null
            result?.utbetalt_tom shouldBe tom
            result?.forelopig_beregnet_slutt shouldBe forelopigBeregnetSluttPaSykepengerLengst
        }

        test("Selects correct utebetalt_tom when soknad contains feriedager") {
            val fnr = "12121212121"
            val forelopigBeregnetSluttPaSykepenger = LocalDate.now().plusDays(15)
            val fom = LocalDate.now().minusDays(3)
            val tom = LocalDate.now().plusDays(7)
            val utbetalingsdagerMedFeriedager =
                createUtbetalingsdager(fom, tom.minusDays(3), DagType.NavDag) +
                    createUtbetalingsdager(tom.minusDays(2), tom, DagType.Feriedag)
            val utb =
                UtbetalingSpleis(
                    fødselsnummer = fnr,
                    organisasjonsnummer = "123123",
                    event = "event",
                    type = "type",
                    fom = fom.toString(),
                    tom = tom.toString(),
                    foreløpigBeregnetSluttPåSykepenger = forelopigBeregnetSluttPaSykepenger.toString(),
                    forbrukteSykedager = 4,
                    gjenståendeSykedager = 5,
                    stønadsdager = 5,
                    antallVedtak = 5,
                    utbetalingId = "123456",
                    korrelasjonsId = "654321",
                    utbetalingsdager = utbetalingsdagerMedFeriedager,
                )

            spleisRecordProcessor.processUtbetalingSpleisEvent(utb)
            val result = utbetalingerDAO.fetchMaksDatoByFnr(fnr = fnr)

            result shouldNotBe null
            result?.utbetalt_tom shouldBe tom.minusDays(3)
            result?.forelopig_beregnet_slutt shouldBe forelopigBeregnetSluttPaSykepenger
        }
    }

    private fun createUtbetalingsdager(fom: LocalDate, tom: LocalDate, dagType: DagType = DagType.NavDag) =
        generateSequence(fom) { it.plusDays(1) }
            .takeWhile { !it.isAfter(tom) }.toList()
            .map { UtbetalingsdagDto(it, dagType, emptyList()) }
}
