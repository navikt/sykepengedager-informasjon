package no.nav.syfo.db

import java.time.LocalDate

data class PSykepengedagerInformasjonSentStatus(
    val uuid: String,
    val eventId: String,
    val createdAt: LocalDate,
    val errorMessage: String,
)
