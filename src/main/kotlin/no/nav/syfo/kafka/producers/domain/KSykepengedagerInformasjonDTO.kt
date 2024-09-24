package no.nav.syfo.kafka.producers.domain

import java.time.LocalDate
import java.time.LocalDateTime

data class KSykepengedagerInformasjonDTO(
    val id: String,
    val personIdent: String,
    val forelopigBeregnetSlutt: LocalDate,
    val utbetaltTom: LocalDate,
    val gjenstaendeSykedager: String,
    val createdAt: LocalDateTime,
)
