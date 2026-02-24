package io.satkuppu.learn.spring_cloud_pubsub.order.service

import io.satkuppu.learn.spring_cloud_pubsub.order.dto.CreateOrderRequest
import io.satkuppu.learn.spring_cloud_pubsub.order.dto.OrderResponse
import io.satkuppu.learn.spring_cloud_pubsub.order.dto.UpdateOrderRequest
import io.satkuppu.learn.spring_cloud_pubsub.order.event.OrderReceivedEvent
import io.satkuppu.learn.spring_cloud_pubsub.order.exception.OrderNotFoundException
import io.satkuppu.learn.spring_cloud_pubsub.order.model.Order
import io.satkuppu.learn.spring_cloud_pubsub.order.repository.OrderRepository
import org.slf4j.LoggerFactory
import org.springframework.cloud.stream.function.StreamBridge
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import java.time.Instant
import java.util.UUID

@Service
class OrderService(
    private val orderRepository: OrderRepository,
    private val streamBridge: StreamBridge
) {
    private val logger = LoggerFactory.getLogger(OrderService::class.java)

    @Transactional
    fun createOrder(request: CreateOrderRequest): OrderResponse {
        val order = Order(
            customerName = request.customerName,
            product = request.product,
            quantity = request.quantity,
            price = request.price
        )
        val saved = orderRepository.save(order)

        val event = OrderReceivedEvent(
            orderId = requireNotNull(saved.id) { "Saved order must have an id" },
            customerName = saved.customerName,
            product = saved.product,
            quantity = saved.quantity,
            price = saved.price,
            timestamp = Instant.now()
        )

        TransactionSynchronizationManager.registerSynchronization(object : TransactionSynchronization {
            override fun afterCommit() {
                val sent = streamBridge.send("order-out-0", event)
                logger.info("OrderReceivedEvent sent to order-out-0: success={}, orderId={}", sent, saved.id)
            }
        })

        return OrderResponse.from(saved)
    }

    @Transactional(readOnly = true)
    fun getAllOrders(): List<OrderResponse> =
        orderRepository.findAll().map { OrderResponse.from(it) }

    @Transactional(readOnly = true)
    fun getOrderById(id: UUID): OrderResponse {
        val order = orderRepository.findByIdOrNull(id)
            ?: throw OrderNotFoundException(id)
        return OrderResponse.from(order)
    }

    @Transactional
    fun updateOrder(id: UUID, request: UpdateOrderRequest): OrderResponse {
        val order = orderRepository.findByIdOrNull(id)
            ?: throw OrderNotFoundException(id)
        order.customerName = request.customerName
        order.product = request.product
        order.quantity = request.quantity
        order.price = request.price
        order.status = request.status
        val updated = orderRepository.save(order)
        return OrderResponse.from(updated)
    }

    @Transactional
    fun deleteOrder(id: UUID) {
        val order = orderRepository.findByIdOrNull(id)
            ?: throw OrderNotFoundException(id)
        orderRepository.delete(order)
    }
}
