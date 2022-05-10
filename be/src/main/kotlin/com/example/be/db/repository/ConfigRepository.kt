package com.example.be.db.repository

import com.example.be.db.generated.tables.daos.ConfigDao
import com.example.be.db.generated.tables.pojos.Config
import com.example.be.db.generated.tables.references.CONFIG
import org.jooq.Configuration
import org.springframework.stereotype.Repository

@Repository
open class ConfigRepository(configuration: Configuration) : ConfigDao(configuration) {

    fun findByName(name: String): Config? {
        return ctx().selectFrom(CONFIG)
            .where(CONFIG.NAME.eq(name))
            .limit(1)
            .fetchOne(mapper())
    }
}
