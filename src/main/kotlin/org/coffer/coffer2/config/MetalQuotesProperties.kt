package org.coffer.coffer2.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "coffer.metal-quotes")
data class MetalQuotesProperties(
    val updateIntervalCron: String,
    val trackedMetalCodes: List<String>
)
