package no.nav.syfo.db.util

import org.flywaydb.core.Flyway
import org.springframework.stereotype.Component

@Component
class DatabaseCleaner(private val flyway: Flyway) {
    fun clean() {
        flyway.clean()
        flyway.migrate()
    }
}
