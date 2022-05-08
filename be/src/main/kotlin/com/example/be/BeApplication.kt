package com.example.be

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

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
                    )
            }
        }
    }

    @Bean
    fun staticResourcesConfigurer(): WebMvcConfigurer {
        return object : WebMvcConfigurer {
            override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
                registry.addResourceHandler("/static/**").addResourceLocations("classpath:/public/static/");
                registry.addResourceHandler("/sw.js").addResourceLocations("classpath:/public/sw.js");
            }
        }
    }
}

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
