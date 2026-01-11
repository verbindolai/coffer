package org.coffer.coffer2.api

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.coffer.coffer2.application.CoinService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/coins")
@Tag(name = "Coins", description = "Coin collection management endpoints")
class CoinController(
    private val coinService: CoinService
) {

    @PostMapping
    @Operation(summary = "Create a new coin", description = "Add a new coin to the collection")
    fun createCoin(@Valid @RequestBody request: CreateCoinRequest): ResponseEntity<CoinResponse> {
        val command = request.toCommand()
        val coin = coinService.createCoin(command)
        return ResponseEntity.status(HttpStatus.CREATED).body(CoinResponse.from(coin))
    }


}