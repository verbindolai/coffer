package org.coffer.coffer2.remote.numista

import feign.RequestInterceptor
import org.springframework.beans.factory.annotation.Value
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam

@FeignClient(
    name = "numista-client",
    url = "\${numista.api.base-url}",
    configuration = [NumistaClientConfig::class],
)
interface NumistaClient {
    @GetMapping("/v3/types/{typeId}")
    fun getCoinType(
        @PathVariable typeId: String,
    ): NumistaTypeResponse

    @GetMapping("/v3/types")
    fun searchTypes(
        @RequestParam("q") query: String,
        @RequestParam("page") page: Int,
        @RequestParam("count") count: Int,
    ): NumistaSearchResponseDto

    @GetMapping("/v3/types/{typeId}/issues")
    fun getIssues(
        @PathVariable typeId: String,
    ): List<NumistaIssueResponse>

    @GetMapping("/v3/types/{typeId}/issues/{issueId}/prices")
    fun getPricesByIssue(
        @PathVariable typeId: String,
        @PathVariable issueId: String,
    ): NumistaPriceResponse
}

@Configuration
class NumistaClientConfig {
    @Value("\${numista.api.key}")
    private lateinit var apiKey: String

    @Bean
    fun numistaApiKeyInterceptor(): RequestInterceptor =
        RequestInterceptor { template ->
            template.header("Numista-API-Key", apiKey)
        }
}
