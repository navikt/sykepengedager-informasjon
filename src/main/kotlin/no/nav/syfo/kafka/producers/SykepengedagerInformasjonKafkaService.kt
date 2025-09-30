package no.nav.syfo.kafka.producers

import no.nav.syfo.db.UtbetalingerDAO
import no.nav.syfo.kafka.producers.domain.KSykepengedagerInformasjonDTO
import no.nav.syfo.logger
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class SykepengedagerInformasjonKafkaService(
    private val utbetalingerDAO: UtbetalingerDAO,
    private val sykepengedagerInformasjonProducer: SykepengedagerInformasjonProducer,
) {
    val log = logger()

    private fun getEventDTO(fnr: String): KSykepengedagerInformasjonDTO {
        val maksDato = utbetalingerDAO.fetchMaksDatoByFnrForKafka(fnr)

        return if (maksDato != null) {
            KSykepengedagerInformasjonDTO(
                id = maksDato.id,
                personIdent = maksDato.fnr,
                forelopigBeregnetSlutt = maksDato.forelopig_beregnet_slutt,
                utbetaltTom = maksDato.tom,
                gjenstaendeSykedager = maksDato.gjenstaende_sykedager,
                createdAt = LocalDateTime.now(),
            )
        } else {
            log.error("Could not map utbetalinger to KSykepengedagerInformasjonDTO, should not happen in prod.")
            throw IllegalArgumentException("Could not map utbetalinger to KSykepengedagerInformasjonDTO")
        }
    }

    fun publishSykepengedagerInformasjonEvent(fnr: String) {
        val event = getEventDTO(fnr)
        sykepengedagerInformasjonProducer.publishSykepengedagerInformasjon(event)
    }
}
