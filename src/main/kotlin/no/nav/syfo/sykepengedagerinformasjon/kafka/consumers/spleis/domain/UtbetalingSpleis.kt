package no.nav.syfo.sykepengedagerinformasjon.kafka.consumers.spleis.domain

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@Suppress("ConstructorParameterNaming")
@JsonIgnoreProperties(ignoreUnknown = true)
data class UtbetalingSpleis(
    val fødselsnummer: String,
    val organisasjonsnummer: String? = null,
    val event: String,
    val type: String? = null,
    val foreløpigBeregnetSluttPåSykepenger: String? = null,
    val forbrukteSykedager: Int? = null,
    val gjenståendeSykedager: Int? = null,
    val stønadsdager: Int? = null,
    val antallVedtak: Int? = null,
    val fom: String,
    val tom: String,
    val utbetalingId: String,
    val korrelasjonsId: String,
)

const val UTBETALING_UTBETALT = "utbetaling_utbetalt"
const val UTBETALING_UTEN_UTBETALING = "utbetaling_uten_utbetaling"
