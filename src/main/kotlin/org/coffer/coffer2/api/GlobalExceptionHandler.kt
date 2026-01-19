package org.coffer.coffer2.api

import io.swagger.v3.oas.annotations.media.Schema
import org.coffer.coffer2.domain.exception.CoinNotFoundException
import org.coffer.coffer2.domain.exception.CofferDomainException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(CoinNotFoundException::class)
    fun handleCoinNotFoundException(ex: CoinNotFoundException): ResponseEntity<ErrorResponse> {
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(ErrorResponse(ex.message ?: "Resource not found"))
    }

    @ExceptionHandler(CofferDomainException::class)
    fun handleDomainException(ex: CofferDomainException): ResponseEntity<ErrorResponse> {
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse(ex.message ?: "Domain error"))
    }
}

@Schema(description = "Error response returned for failed requests")
data class ErrorResponse(
    @Schema(description = "Human-readable error message", example = "Coin not found with id: 550e8400-e29b-41d4-a716-446655440000")
    val message: String
)
