package org.coffer.coffer2.application

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "coffer.storage")
data class StorageProperties(
    var basePath: String = "./data/images",
    var maxFileSizeBytes: Long = 10485760, // 10MB
    var allowedContentTypes: List<String> = listOf("image/jpeg", "image/png", "image/webp")
)