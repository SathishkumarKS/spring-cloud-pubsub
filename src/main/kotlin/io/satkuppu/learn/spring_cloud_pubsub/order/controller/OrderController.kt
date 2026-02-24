package io.satkuppu.learn.spring_cloud_pubsub.order.controller

import io.satkuppu.learn.spring_cloud_pubsub.order.dto.CreateOrderRequest
import io.satkuppu.learn.spring_cloud_pubsub.order.dto.OrderResponse
import io.satkuppu.learn.spring_cloud_pubsub.order.dto.UpdateOrderRequest
import io.satkuppu.learn.spring_cloud_pubsub.order.service.OrderService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/orders")
class OrderController(private val orderService: OrderService) {

    @PostMapping
    fun createOrder(@Valid @RequestBody request: CreateOrderRequest): ResponseEntity<OrderResponse> {
        val order = orderService.createOrder(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(order)
    }

    @GetMapping
    fun getAllOrders(): ResponseEntity<List<OrderResponse>> =
        ResponseEntity.ok(orderService.getAllOrders())

    @GetMapping("/{id}")
    fun getOrderById(@PathVariable id: UUID): ResponseEntity<OrderResponse> =
        ResponseEntity.ok(orderService.getOrderById(id))

    @PutMapping("/{id}")
    fun updateOrder(
        @PathVariable id: UUID,
        @Valid @RequestBody request: UpdateOrderRequest
    ): ResponseEntity<OrderResponse> =
        ResponseEntity.ok(orderService.updateOrder(id, request))

    @DeleteMapping("/{id}")
    fun deleteOrder(@PathVariable id: UUID): ResponseEntity<Void> {
        orderService.deleteOrder(id)
        return ResponseEntity.noContent().build()
    }
}
