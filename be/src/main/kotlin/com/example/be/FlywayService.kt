package com.example.be

import org.flywaydb.core.Flyway
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import javax.sql.DataSource

@Component
class FlywayService(
    val dataSource: DataSource
) {

    @EventListener(ApplicationReadyEvent::class)
    fun flyway() {
        val flyway = Flyway.configure().dataSource(dataSource).load()
        flyway.migrate()
    }
}
