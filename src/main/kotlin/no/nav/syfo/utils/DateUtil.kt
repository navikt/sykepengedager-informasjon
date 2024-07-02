package no.nav.syfo.utils

import java.time.LocalDate
import java.time.format.DateTimeFormatter

private const val BREV_DATE_FORMAT_PATTERN = "dd. MMMM yyyy"

fun LocalDate.isEqualOrBefore(other: LocalDate): Boolean {
    return this == other || this.isBefore(other)
}

fun parseDate(date: String): LocalDate {
    val formatter = DateTimeFormatter.ofPattern("yyyyMMdd")
    return LocalDate.parse(date, formatter)
}

fun formatDateForLetter(date: LocalDate): String {
    return date.format(DateTimeFormatter.ofPattern(BREV_DATE_FORMAT_PATTERN))
}
