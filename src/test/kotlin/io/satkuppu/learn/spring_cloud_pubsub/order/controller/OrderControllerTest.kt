package io.satkuppu.learn.spring_cloud_pubsub.order.controller

import com.fasterxml.jackson.databind.ObjectMapper
import io.satkuppu.learn.spring_cloud_pubsub.order.dto.CreateOrderRequest
import io.satkuppu.learn.spring_cloud_pubsub.order.dto.OrderResponse
import io.satkuppu.learn.spring_cloud_pubsub.order.dto.UpdateOrderRequest
import io.satkuppu.learn.spring_cloud_pubsub.order.exception.OrderExceptionHandler
import io.satkuppu.learn.spring_cloud_pubsub.order.exception.OrderNotFoundException
import io.satkuppu.learn.spring_cloud_pubsub.order.model.OrderStatus
import io.satkuppu.learn.spring_cloud_pubsub.order.service.OrderService
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@WebMvcTest(OrderController::class)
@Import(OrderExceptionHandler::class)
class OrderControllerTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @MockitoBean
    lateinit var orderService: OrderService

    private val sampleId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000")
    private val fixedInstant = Instant.parse("2026-01-01T00:00:00Z")

    private fun sampleOrderResponse(id: UUID = sampleId) = OrderResponse(
        id = id,
        customerName = "John Doe",
        product = "Widget",
        quantity = 2,
        price = BigDecimal("19.99"),
        status = OrderStatus.CREATED,
        createdAt = fixedInstant,
        updatedAt = fixedInstant
    )

    @Test
    fun `POST api_orders creates order and returns 201`() {
        val request = CreateOrderRequest("John Doe", "Widget", 2, BigDecimal("19.99"))
        whenever(orderService.createOrder(any())).thenReturn(sampleOrderResponse())

        mockMvc.perform(
            post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").value(sampleId.toString()))
            .andExpect(jsonPath("$.customerName").value("John Doe"))
            .andExpect(jsonPath("$.product").value("Widget"))
            .andExpect(jsonPath("$.quantity").value(2))
            .andExpect(jsonPath("$.status").value("CREATED"))
    }

    @Test
    fun `POST api_orders returns 400 for blank customerName`() {
        val body = """{"customerName":"","product":"Widget","quantity":2,"price":19.99}"""

        mockMvc.perform(
            post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value("Bad Request"))
            .andExpect(jsonPath("$.errors").isArray)
    }

    @Test
    fun `POST api_orders returns 400 for negative quantity`() {
        val body = """{"customerName":"John","product":"Widget","quantity":-1,"price":19.99}"""

        mockMvc.perform(
            post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value("Bad Request"))
    }

    @Test
    fun `POST api_orders returns 400 for zero price`() {
        val body = """{"customerName":"John","product":"Widget","quantity":1,"price":0.00}"""

        mockMvc.perform(
            post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `POST api_orders returns 400 for malformed JSON`() {
        mockMvc.perform(
            post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{invalid json}")
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value("Bad Request"))
            .andExpect(jsonPath("$.message").value("Malformed JSON request"))
    }

    @Test
    fun `GET api_orders returns list of orders`() {
        whenever(orderService.getAllOrders()).thenReturn(listOf(sampleOrderResponse()))

        mockMvc.perform(get("/api/orders"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].id").value(sampleId.toString()))
            .andExpect(jsonPath("$[0].customerName").value("John Doe"))
    }

    @Test
    fun `GET api_orders_id returns order when found`() {
        whenever(orderService.getOrderById(sampleId)).thenReturn(sampleOrderResponse())

        mockMvc.perform(get("/api/orders/$sampleId"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(sampleId.toString()))
            .andExpect(jsonPath("$.customerName").value("John Doe"))
    }

    @Test
    fun `GET api_orders_id returns 404 when not found`() {
        whenever(orderService.getOrderById(sampleId)).thenThrow(OrderNotFoundException(sampleId))

        mockMvc.perform(get("/api/orders/$sampleId"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.error").value("Not Found"))
            .andExpect(jsonPath("$.message").value("Order not found with id: $sampleId"))
    }

    @Test
    fun `GET api_orders_id returns 400 for invalid UUID`() {
        mockMvc.perform(get("/api/orders/not-a-uuid"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value("Bad Request"))
    }

    @Test
    fun `PUT api_orders_id updates order when found`() {
        val request = UpdateOrderRequest("Jane Updated", "Updated Widget", 10, BigDecimal("29.99"), OrderStatus.CONFIRMED)
        val updatedResponse = sampleOrderResponse().copy(
            customerName = "Jane Updated",
            product = "Updated Widget",
            quantity = 10,
            price = BigDecimal("29.99"),
            status = OrderStatus.CONFIRMED
        )
        whenever(orderService.updateOrder(eq(sampleId), any())).thenReturn(updatedResponse)

        mockMvc.perform(
            put("/api/orders/$sampleId")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.customerName").value("Jane Updated"))
            .andExpect(jsonPath("$.status").value("CONFIRMED"))
    }

    @Test
    fun `PUT api_orders_id returns 404 when not found`() {
        whenever(orderService.updateOrder(eq(sampleId), any())).thenThrow(OrderNotFoundException(sampleId))

        val request = UpdateOrderRequest("a", "b", 1, BigDecimal("1.00"), OrderStatus.CREATED)
        mockMvc.perform(
            put("/api/orders/$sampleId")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.error").value("Not Found"))
    }

    @Test
    fun `PUT api_orders_id returns 400 for invalid request`() {
        val body = """{"customerName":"","product":"","quantity":-1,"price":0,"status":"CREATED"}"""

        mockMvc.perform(
            put("/api/orders/$sampleId")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value("Bad Request"))
    }

    @Test
    fun `DELETE api_orders_id returns 204 when found`() {
        mockMvc.perform(delete("/api/orders/$sampleId"))
            .andExpect(status().isNoContent)
    }

    @Test
    fun `DELETE api_orders_id returns 404 when not found`() {
        whenever(orderService.deleteOrder(sampleId)).thenThrow(OrderNotFoundException(sampleId))

        mockMvc.perform(delete("/api/orders/$sampleId"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.error").value("Not Found"))
    }
}
