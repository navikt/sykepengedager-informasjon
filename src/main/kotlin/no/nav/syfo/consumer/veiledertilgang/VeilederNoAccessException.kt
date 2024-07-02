package no.nav.syfo.consumer.veiledertilgang

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(value = HttpStatus.FORBIDDEN, reason = "Cannot fetch max date: Veileder has no access to person")
class VeilederNoAccessException : RuntimeException()
