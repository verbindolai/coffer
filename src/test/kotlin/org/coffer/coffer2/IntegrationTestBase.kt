package org.coffer.coffer2

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.PostConstruct
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.test.context.ActiveProfiles
import org.testcontainers.containers.PostgreSQLContainer

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
abstract class IntegrationTestBase {

    private val logger = KotlinLogging.logger {}


    companion object {

        const val POSTGRES_IMAGE_NAME = "postgres:14.5"


        @ServiceConnection
        val postgresContainer: PostgreSQLContainer<*> = PostgreSQLContainer("postgres:17-alpine").apply {
            withDatabaseName("coffer2_test")
            withUsername("test")
            withPassword("test")
            withReuse(true)
            start()
        }
    }

    @PostConstruct
    fun initialize() {
        logger.info { "Test database initialized with:" }
        logger.info { "JDBC URL: ${postgresContainer.jdbcUrl}" }
        logger.info { "Username: ${postgresContainer.username}" }
        logger.info { "Password: ${postgresContainer.password}" }
    }
}

