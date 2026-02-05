package org.coffer.coffer2.remote.numista

import org.coffer.coffer2.config.NumistaOAuthProperties
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

@Component
class NumistaOAuthClient(
    private val properties: NumistaOAuthProperties,
) {
    private val restClient = RestClient.builder()
        .baseUrl(properties.baseUrl)
        .build()

    fun exchangeCode(code: String, redirectUri: String): NumistaOAuthTokenResponse {
        return restClient.get()
            .uri { uriBuilder ->
                uriBuilder.path("/v3/oauth_token")
                    .queryParam("grant_type", "authorization_code")
                    .queryParam("code", code)
                    .queryParam("client_id", properties.clientId)
                    .queryParam("client_secret", properties.key)
                    .queryParam("redirect_uri", redirectUri)
                    .build()
            }
            .retrieve()
            .body(NumistaOAuthTokenResponse::class.java)!!
    }

    fun getCollectedItems(userId: Int, accessToken: String): NumistaCollectedItemsResponse {
        return restClient.get()
            .uri { uriBuilder ->
                uriBuilder.path("/v3/users/{userId}/collected_items")
                    .queryParam("category", "coin")
                    .build(userId)
            }
            .header("Authorization", "Bearer $accessToken")
            .header("Numista-API-Key", properties.key)
            .retrieve()
            .body(NumistaCollectedItemsResponse::class.java)!!
    }
}
