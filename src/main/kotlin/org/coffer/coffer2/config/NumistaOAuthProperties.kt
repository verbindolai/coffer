package org.coffer.coffer2.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "numista.api")
data class NumistaOAuthProperties(
    val baseUrl: String,
    val key: String,
    val clientId: String,
    val redirectUri: String,
)
