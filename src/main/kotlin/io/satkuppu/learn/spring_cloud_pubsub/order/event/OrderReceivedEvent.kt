package io.satkuppu.learn.spring_cloud_pubsub.order.event

import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

data class OrderReceivedEvent(
    val orderId: UUID,
    val customerName: String,
    val product: String,
    val quantity: Int,
    val price: BigDecimal,
    val timestamp: Instant
)
