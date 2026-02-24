package io.satkuppu.learn.spring_cloud_pubsub.order.repository

import io.satkuppu.learn.spring_cloud_pubsub.order.model.Order
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface OrderRepository : JpaRepository<Order, UUID>
