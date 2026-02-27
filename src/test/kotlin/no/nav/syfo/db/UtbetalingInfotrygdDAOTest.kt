package no.nav.syfo.db

import io.kotest.core.extensions.ApplyExtension
import io.kotest.core.spec.style.FunSpec
import io.kotest.core.test.TestCase
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.syfo.LocalApplication
import no.nav.syfo.db.util.DatabaseCleaner
import no.nav.syfo.kafka.consumers.aapInfotrygd.domain.InfotrygdSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.time.LocalDate

@SpringBootTest(classes = [LocalApplication::class])
@ApplyExtension(SpringExtension::class)
class UtbetalingInfotrygdDAOTest : FunSpec() {
    @Autowired
    private lateinit var utbetalingInfotrygdDAO: UtbetalingInfotrygdDAO

    @Autowired
    private lateinit var databaseCleaner: DatabaseCleaner

    override suspend fun beforeTest(testCase: TestCase) {
        databaseCleaner.clean()
    }

    init {
        val fnr = "12121212121"
        val maxDate = LocalDate.now().plusDays(30)
        val gjenstaendeSykedager = 5

        test("Store utbetaling Infotrygd") {
            utbetalingInfotrygdDAO.storeInfotrygdUtbetaling(
                fnr,
                maxDate,
                LocalDate.now().plusDays(20),
                gjenstaendeSykedager,
                InfotrygdSource.AAP_KAFKA_TOPIC
            )

            val result =
                utbetalingInfotrygdDAO.fetchInfotrygdUtbetalingByFnr(
                    fnr = fnr,
                )
            result shouldNotBe null
            result?.first shouldBe fnr
            result?.second shouldBe maxDate.toString()
            result?.third shouldBe gjenstaendeSykedager
        }
    }
}
