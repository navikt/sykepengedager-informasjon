package no.nav.syfo.db.util

import org.flywaydb.core.Flyway
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class DatabaseCleaner(@Autowired private val flyway: Flyway) {
    fun clean() {
        flyway.clean()
        flyway.migrate()
    }
}
