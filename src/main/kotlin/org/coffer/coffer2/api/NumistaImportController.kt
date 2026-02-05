package org.coffer.coffer2.api

import org.coffer.coffer2.application.numistaimport.NumistaImportService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/import/numista")
class NumistaImportController(
    private val importService: NumistaImportService,
) {

    @GetMapping("/auth-url")
    fun getAuthUrl(): NumistaAuthUrlResponse {
        val state = UUID.randomUUID().toString()
        val url = importService.getAuthorizationUrl(state)
        return NumistaAuthUrlResponse(url)
    }

    @PostMapping("/callback")
    fun importCollection(@RequestBody request: NumistaImportCallbackRequest): NumistaImportResultResponse {
        val result = importService.importCollection(request.code, request.redirectUri)
        return NumistaImportResultResponse(
            imported = result.imported,
            skipped = result.skipped,
            failed = result.failed,
            errors = result.errors,
        )
    }
}
