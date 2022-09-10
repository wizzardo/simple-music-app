package com.example.be

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.core.env.Environment
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import org.springframework.web.servlet.resource.EncodedResourceResolver
import org.springframework.web.servlet.resource.PathResourceResolver
import javax.sql.ConnectionPoolDataSource

@SpringBootApplication
class BeApplication {

    @Bean
    fun corsConfigurer(): WebMvcConfigurer {
        return object : WebMvcConfigurer {
            override fun addCorsMappings(registry: CorsRegistry?) {
                registry!!
                    .addMapping("/**")
                    .allowCredentials(true)
                    .allowedMethods("GET", "HEAD", "POST", "PUT", "DELETE", "OPTIONS")
                    .allowedOrigins(
                        "http://localhost:3000",
                        "http://localhost:8080",
                        "http://192.168.0.147:3000",
                    )
            }
        }
    }

    @Bean
    fun staticResourcesConfigurer(): WebMvcConfigurer {
        return object : WebMvcConfigurer {
            override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
                registry.addResourceHandler("/static/**")
                    .addResourceLocations("classpath:/public/static/")
                    .resourceChain(true) // cache resource lookups
                    .addResolver(EncodedResourceResolver())
                    .addResolver(PathResourceResolver())

                registry.addResourceHandler("/sw.js").addResourceLocations("classpath:/public/sw.js");
                registry.addResourceHandler("/manifest.json").addResourceLocations("classpath:/public/manifest.json");
            }
        }
    }

    @Bean
    fun createDatasource(env: Environment): ConnectionPoolDataSource {
        val poolDataSource = org.postgresql.ds.PGConnectionPoolDataSource()

        var username = env.getProperty("spring.datasource.username")
        var password = env.getProperty("spring.datasource.password")
        var url = env.getProperty("spring.datasource.url")

        val dbUrl = System.getenv("DATABASE_URL")
        if (dbUrl != null) {
            val credentials = dbUrl.substringAfter("://").substringBefore("@")
            url = "jdbc:postgresql://" + dbUrl.substringAfter("@")
            username = credentials.substringBefore(":")
            password = credentials.substringAfter(":")
        }

        val dbName = url.substringAfterLast("/")
        val host = url.substringAfter("://").substringBefore("/")

        poolDataSource.databaseName = dbName
        poolDataSource.serverNames = arrayOf(host.substringBefore(":"))
        poolDataSource.portNumbers = intArrayOf(host.substringAfter(":").toIntOrNull() ?: 5432)
        poolDataSource.user = username
        poolDataSource.password = password
        poolDataSource.binaryTransfer = true
//        poolDataSource.ssl = true
//        poolDataSource.sslMode = "require"
        poolDataSource.tcpKeepAlive = true
        poolDataSource.preparedStatementCacheSizeMiB = 1
        poolDataSource.preparedStatementCacheQueries = 32

        return poolDataSource
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val dbUrl = System.getenv("DATABASE_URL")
            if (dbUrl != null) {
                val credentials = dbUrl.substringAfter("://").substringBefore("@")
                val url = "jdbc:postgresql://" + dbUrl.substringAfter("@")
                System.setProperty("spring.datasource.url", url)
                System.setProperty("spring.datasource.username", credentials.substringBefore(":"))
                System.setProperty("spring.datasource.password", credentials.substringAfter(":"))
            }

            val port = System.getenv("PORT")
            if (port != null) {
                System.setProperty("server.port", port)
            }

            runApplication<BeApplication>(*args)
        }
    }
}

