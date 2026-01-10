package org.coffer.coffer2.remote.swissquote

import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable

@FeignClient(
    name = "swissquote-client",
    url = "\${swissquote.api.base-url}",
)
interface SwissquoteClient {
    /**
     * Fetch current metal price from Swissquote API
     * @param metal XAU for gold, XAG for silver
     * @return Price data or null if unavailable
     */
    @GetMapping("/public-quotes/bboquotes/instrument/{metal}/EUR")
    fun getMetalPrice(
        @PathVariable("metal") metal: String,
    ): List<SwissquoteQuoteResponse>
}
