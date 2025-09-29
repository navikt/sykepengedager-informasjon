package no.nav.syfo.kafka.consumers.spleis.domain

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.time.LocalDate

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
    val utbetalingsdager: List<UtbetalingsdagDto> = emptyList(),
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class UtbetalingsdagDto(
    val dato: LocalDate,
    val type: DagType,
    val begrunnelser: List<String>,
)

enum class DagType {
    ArbeidsgiverperiodeDag,
    NavDag,
    NavHelgDag,
    Arbeidsdag,
    Fridag,
    AvvistDag,
    UkjentDag,
    ForeldetDag,
    Permisjonsdag,
    Feriedag,
    ArbeidIkkeGjenopptattDag,
    AndreYtelser,
    Venteperiodedag,
    Ventetidsdag,

    @JsonEnumDefaultValue
    Ukjent,
}

/**
Begrunnelse:
AndreYtelserAap,
AndreYtelserDagpenger,
AndreYtelserForeldrepenger,
AndreYtelserOmsorgspenger,
AndreYtelserOpplaringspenger,
AndreYtelserPleiepenger,
AndreYtelserSvangerskapspenger,
SykepengedagerOppbrukt,
SykepengedagerOppbruktOver67,
MinimumInntekt,
EgenmeldingUtenforArbeidsgiverperiode,
MinimumSykdomsgrad,
ManglerOpptjening,
ManglerMedlemskap,
EtterDødsdato,
Over70,
MinimumInntektOver67,
NyVilkårsprøvingNødvendig,
UKJENT
 **/

const val UTBETALING_UTBETALT = "utbetaling_utbetalt"
const val UTBETALING_UTEN_UTBETALING = "utbetaling_uten_utbetaling"
