package io.satkuppu.learn.spring_cloud_pubsub.order.exception

import java.util.UUID

class OrderNotFoundException(id: UUID) : RuntimeException("Order not found with id: $id")
