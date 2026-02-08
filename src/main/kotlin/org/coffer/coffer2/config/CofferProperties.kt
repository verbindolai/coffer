package org.coffer.coffer2.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "coffer")
data class CofferProperties(
    val baseUrlFrontend: String,
)
