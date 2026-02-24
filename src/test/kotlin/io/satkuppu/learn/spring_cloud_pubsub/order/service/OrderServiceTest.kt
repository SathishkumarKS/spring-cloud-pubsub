package io.satkuppu.learn.spring_cloud_pubsub.order.service

import io.satkuppu.learn.spring_cloud_pubsub.order.dto.CreateOrderRequest
import io.satkuppu.learn.spring_cloud_pubsub.order.dto.UpdateOrderRequest
import io.satkuppu.learn.spring_cloud_pubsub.order.event.OrderReceivedEvent
import io.satkuppu.learn.spring_cloud_pubsub.order.exception.OrderNotFoundException
import io.satkuppu.learn.spring_cloud_pubsub.order.model.Order
import io.satkuppu.learn.spring_cloud_pubsub.order.model.OrderStatus
import io.satkuppu.learn.spring_cloud_pubsub.order.repository.OrderRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.cloud.stream.function.StreamBridge
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import java.math.BigDecimal
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@ExtendWith(MockitoExtension::class)
class OrderServiceTest {

    @Mock
    lateinit var orderRepository: OrderRepository

    @Mock
    lateinit var streamBridge: StreamBridge

    @InjectMocks
    lateinit var orderService: OrderService

    @BeforeEach
    fun setUp() {
        TransactionSynchronizationManager.initSynchronization()
    }

    @AfterEach
    fun tearDown() {
        TransactionSynchronizationManager.clearSynchronization()
    }

    private fun flushAfterCommitCallbacks() {
        TransactionSynchronizationManager.getSynchronizations().forEach {
            it.afterCommit()
        }
    }

    private fun createSampleOrder(id: UUID = UUID.randomUUID()): Order = Order(
        id = id,
        customerName = "John Doe",
        product = "Widget",
        quantity = 2,
        price = BigDecimal("19.99")
    )

    @Test
    fun `createOrder saves order and publishes event after commit`() {
        val request = CreateOrderRequest(
            customerName = "John Doe",
            product = "Widget",
            quantity = 2,
            price = BigDecimal("19.99")
        )
        val savedOrder = createSampleOrder()

        whenever(orderRepository.save(any<Order>())).thenReturn(savedOrder)
        whenever(streamBridge.send(eq("order-out-0"), any<OrderReceivedEvent>())).thenReturn(true)

        val response = orderService.createOrder(request)

        assertEquals("John Doe", response.customerName)
        assertEquals("Widget", response.product)
        assertEquals(2, response.quantity)
        assertEquals(BigDecimal("19.99"), response.price)
        assertEquals(OrderStatus.CREATED, response.status)

        verify(orderRepository).save(any<Order>())

        // Event is sent only after commit
        flushAfterCommitCallbacks()
        verify(streamBridge).send(eq("order-out-0"), any<OrderReceivedEvent>())
    }

    @Test
    fun `createOrder publishes event with correct data after commit`() {
        val request = CreateOrderRequest(
            customerName = "Jane Smith",
            product = "Gadget",
            quantity = 5,
            price = BigDecimal("49.99")
        )
        val orderId = UUID.randomUUID()
        val savedOrder = Order(
            id = orderId,
            customerName = "Jane Smith",
            product = "Gadget",
            quantity = 5,
            price = BigDecimal("49.99")
        )

        whenever(orderRepository.save(any<Order>())).thenReturn(savedOrder)
        whenever(streamBridge.send(eq("order-out-0"), any<OrderReceivedEvent>())).thenReturn(true)

        orderService.createOrder(request)

        flushAfterCommitCallbacks()

        val eventCaptor = ArgumentCaptor.forClass(OrderReceivedEvent::class.java)
        verify(streamBridge).send(eq("order-out-0"), eventCaptor.capture())

        val event = eventCaptor.value
        assertEquals(orderId, event.orderId)
        assertEquals("Jane Smith", event.customerName)
        assertEquals("Gadget", event.product)
        assertEquals(5, event.quantity)
        assertEquals(BigDecimal("49.99"), event.price)
        assertNotNull(event.timestamp)
    }

    @Test
    fun `getAllOrders returns all orders`() {
        val orders = listOf(createSampleOrder(), createSampleOrder())
        whenever(orderRepository.findAll()).thenReturn(orders)

        val result = orderService.getAllOrders()

        assertEquals(2, result.size)
        verify(orderRepository).findAll()
    }

    @Test
    fun `getOrderById returns order when found`() {
        val id = UUID.randomUUID()
        val order = createSampleOrder(id)
        whenever(orderRepository.findById(id)).thenReturn(java.util.Optional.of(order))

        val result = orderService.getOrderById(id)

        assertEquals(id, result.id)
        assertEquals("John Doe", result.customerName)
    }

    @Test
    fun `getOrderById throws OrderNotFoundException when not found`() {
        val id = UUID.randomUUID()
        whenever(orderRepository.findById(id)).thenReturn(java.util.Optional.empty())

        assertThrows<OrderNotFoundException> {
            orderService.getOrderById(id)
        }
    }

    @Test
    fun `updateOrder updates and returns order when found`() {
        val id = UUID.randomUUID()
        val existingOrder = createSampleOrder(id)
        val request = UpdateOrderRequest(
            customerName = "Jane Updated",
            product = "Updated Widget",
            quantity = 10,
            price = BigDecimal("29.99"),
            status = OrderStatus.CONFIRMED
        )

        whenever(orderRepository.findById(id)).thenReturn(java.util.Optional.of(existingOrder))
        whenever(orderRepository.save(any<Order>())).thenReturn(existingOrder)

        orderService.updateOrder(id, request)

        assertEquals("Jane Updated", existingOrder.customerName)
        assertEquals("Updated Widget", existingOrder.product)
        assertEquals(10, existingOrder.quantity)
        assertEquals(BigDecimal("29.99"), existingOrder.price)
        assertEquals(OrderStatus.CONFIRMED, existingOrder.status)
        verify(orderRepository).save(existingOrder)
    }

    @Test
    fun `updateOrder throws OrderNotFoundException when not found`() {
        val id = UUID.randomUUID()
        whenever(orderRepository.findById(id)).thenReturn(java.util.Optional.empty())

        assertThrows<OrderNotFoundException> {
            orderService.updateOrder(id, UpdateOrderRequest("a", "b", 1, BigDecimal.ONE, OrderStatus.CREATED))
        }
    }

    @Test
    fun `deleteOrder deletes when order exists`() {
        val id = UUID.randomUUID()
        val order = createSampleOrder(id)
        whenever(orderRepository.findById(id)).thenReturn(java.util.Optional.of(order))

        orderService.deleteOrder(id)

        verify(orderRepository).delete(order)
    }

    @Test
    fun `deleteOrder throws OrderNotFoundException when not found`() {
        val id = UUID.randomUUID()
        whenever(orderRepository.findById(id)).thenReturn(java.util.Optional.empty())

        assertThrows<OrderNotFoundException> {
            orderService.deleteOrder(id)
        }
    }
}
