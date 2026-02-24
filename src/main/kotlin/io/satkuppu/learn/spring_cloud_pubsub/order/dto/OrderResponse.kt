package io.satkuppu.learn.spring_cloud_pubsub.order.dto

import io.satkuppu.learn.spring_cloud_pubsub.order.model.Order
import io.satkuppu.learn.spring_cloud_pubsub.order.model.OrderStatus
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

data class OrderResponse(
    val id: UUID,
    val customerName: String,
    val product: String,
    val quantity: Int,
    val price: BigDecimal,
    val status: OrderStatus,
    val createdAt: Instant,
    val updatedAt: Instant
) {
    companion object {
        fun from(order: Order): OrderResponse = OrderResponse(
            id = requireNotNull(order.id) { "Cannot create response from unsaved order" },
            customerName = order.customerName,
            product = order.product,
            quantity = order.quantity,
            price = order.price,
            status = order.status,
            createdAt = order.createdAt,
            updatedAt = order.updatedAt
        )
    }
}
