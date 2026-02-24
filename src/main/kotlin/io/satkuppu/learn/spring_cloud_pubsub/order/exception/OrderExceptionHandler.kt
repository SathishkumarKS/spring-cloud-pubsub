package io.satkuppu.learn.spring_cloud_pubsub.order.exception

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import java.time.Instant

@ControllerAdvice
class OrderExceptionHandler {

    @ExceptionHandler(OrderNotFoundException::class)
    fun handleOrderNotFound(ex: OrderNotFoundException): ResponseEntity<Map<String, Any>> {
        val body = mapOf(
            "error" to "Not Found",
            "message" to (ex.message ?: "Order not found"),
            "timestamp" to Instant.now().toString()
        )
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body)
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationErrors(ex: MethodArgumentNotValidException): ResponseEntity<Map<String, Any>> {
        val errors = ex.bindingResult.fieldErrors.map { "${it.field}: ${it.defaultMessage}" }
        val body = mapOf(
            "error" to "Bad Request",
            "message" to "Validation failed",
            "errors" to errors,
            "timestamp" to Instant.now().toString()
        )
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body)
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleMalformedJson(ex: HttpMessageNotReadableException): ResponseEntity<Map<String, Any>> {
        val body = mapOf(
            "error" to "Bad Request",
            "message" to "Malformed JSON request",
            "timestamp" to Instant.now().toString()
        )
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body)
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleTypeMismatch(ex: MethodArgumentTypeMismatchException): ResponseEntity<Map<String, Any>> {
        val body = mapOf(
            "error" to "Bad Request",
            "message" to "Invalid value for parameter '${ex.name}': ${ex.value}",
            "timestamp" to Instant.now().toString()
        )
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body)
    }
}
