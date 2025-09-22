package no.nav.syfo.config

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres
import jakarta.annotation.PreDestroy
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import javax.sql.DataSource

@Configuration
class EmbeddedPostgresTestConfig {
    private lateinit var embeddedPostgresVar: EmbeddedPostgres

    @Bean
    fun embeddedPostgres(): EmbeddedPostgres {
        embeddedPostgresVar = EmbeddedPostgres.builder().start()
        return embeddedPostgresVar
    }

    @Bean
    @Primary
    fun dataSource(pg: EmbeddedPostgres): DataSource = pg.postgresDatabase

    @PreDestroy
    fun shutDown() {
        if (this::embeddedPostgresVar.isInitialized) {
            embeddedPostgresVar.close()
        }
    }
}
