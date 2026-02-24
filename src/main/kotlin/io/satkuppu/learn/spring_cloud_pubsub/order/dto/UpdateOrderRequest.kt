package io.satkuppu.learn.spring_cloud_pubsub.order.dto

import io.satkuppu.learn.spring_cloud_pubsub.order.model.OrderStatus
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import java.math.BigDecimal

data class UpdateOrderRequest(
    @field:NotBlank(message = "Customer name is required")
    val customerName: String,

    @field:NotBlank(message = "Product is required")
    val product: String,

    @field:Positive(message = "Quantity must be positive")
    val quantity: Int,

    @field:DecimalMin(value = "0.01", message = "Price must be at least 0.01")
    val price: BigDecimal,

    @field:NotNull(message = "Status is required")
    val status: OrderStatus
)
