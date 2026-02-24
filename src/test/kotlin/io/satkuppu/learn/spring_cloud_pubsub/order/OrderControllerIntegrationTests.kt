package io.satkuppu.learn.spring_cloud_pubsub.order

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.api.gax.core.CredentialsProvider
import com.google.api.gax.core.NoCredentialsProvider
import com.google.api.gax.grpc.GrpcTransportChannel
import com.google.api.gax.rpc.FixedTransportChannelProvider
import com.google.cloud.pubsub.v1.SubscriptionAdminClient
import com.google.cloud.pubsub.v1.SubscriptionAdminSettings
import com.google.cloud.pubsub.v1.TopicAdminClient
import com.google.cloud.pubsub.v1.TopicAdminSettings
import com.google.cloud.pubsub.v1.stub.GrpcSubscriberStub
import com.google.cloud.pubsub.v1.stub.SubscriberStubSettings
import com.google.pubsub.v1.ProjectSubscriptionName
import com.google.pubsub.v1.PullRequest
import com.google.pubsub.v1.PushConfig
import com.google.pubsub.v1.SubscriptionName
import com.google.pubsub.v1.TopicName
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.satkuppu.learn.spring_cloud_pubsub.order.dto.CreateOrderRequest
import io.satkuppu.learn.spring_cloud_pubsub.order.dto.UpdateOrderRequest
import io.satkuppu.learn.spring_cloud_pubsub.order.model.OrderStatus
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.testcontainers.containers.PubSubEmulatorContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import java.math.BigDecimal
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("integration")
@Testcontainers
class OrderControllerIntegrationTests {

    companion object {
        private const val PROJECT_ID = "test-project"
        private const val HELLO_TOPIC_ID = "hello-topic"
        private const val ORDER_EVENTS_TOPIC_ID = "order-events"
        private const val ORDER_EVENTS_SUBSCRIPTION_ID = "order-events-subscription"

        @Container
        @JvmStatic
        val pubsubEmulator = PubSubEmulatorContainer(
            DockerImageName.parse("gcr.io/google.com/cloudsdktool/google-cloud-cli:emulators")
        )

        @JvmStatic
        @DynamicPropertySource
        fun emulatorProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.cloud.gcp.pubsub.emulator-host") { pubsubEmulator.emulatorEndpoint }
            registry.add("spring.cloud.gcp.project-id") { PROJECT_ID }
        }

        @JvmStatic
        @BeforeAll
        fun setupPubSub() {
            val channel = ManagedChannelBuilder
                .forTarget("dns:///${pubsubEmulator.emulatorEndpoint}")
                .usePlaintext()
                .build()
            val channelProvider = FixedTransportChannelProvider.create(GrpcTransportChannel.create(channel))
            val credentialsProvider = NoCredentialsProvider.create()

            TopicAdminClient.create(
                TopicAdminSettings.newBuilder()
                    .setTransportChannelProvider(channelProvider)
                    .setCredentialsProvider(credentialsProvider)
                    .build()
            ).use { topicAdmin ->
                topicAdmin.createTopic(TopicName.of(PROJECT_ID, HELLO_TOPIC_ID))
                topicAdmin.createTopic(TopicName.of(PROJECT_ID, ORDER_EVENTS_TOPIC_ID))
            }

            SubscriptionAdminClient.create(
                SubscriptionAdminSettings.newBuilder()
                    .setTransportChannelProvider(channelProvider)
                    .setCredentialsProvider(credentialsProvider)
                    .build()
            ).use { subscriptionAdmin ->
                subscriptionAdmin.createSubscription(
                    SubscriptionName.of(PROJECT_ID, ORDER_EVENTS_SUBSCRIPTION_ID),
                    TopicName.of(PROJECT_ID, ORDER_EVENTS_TOPIC_ID),
                    PushConfig.getDefaultInstance(),
                    10
                )
            }

            channel.shutdown().awaitTermination(5, TimeUnit.SECONDS)
        }

        private fun createGrpcChannel(): ManagedChannel =
            ManagedChannelBuilder
                .forTarget("dns:///${pubsubEmulator.emulatorEndpoint}")
                .usePlaintext()
                .build()
    }

    @TestConfiguration
    class PubSubEmulatorConfig {
        @Bean
        fun credentialsProvider(): CredentialsProvider = NoCredentialsProvider.create()
    }

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    private fun createOrder(customerName: String, product: String, quantity: Int, price: BigDecimal): String {
        val request = CreateOrderRequest(customerName, product, quantity, price)
        val result = mockMvc.perform(
            post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)
            .andReturn()
        return objectMapper.readTree(result.response.contentAsString)["id"].asText()
    }

    @Test
    fun `POST api_orders creates order and publishes event to PubSub`() {
        val request = CreateOrderRequest(
            customerName = "Integration Test User",
            product = "Test Product",
            quantity = 3,
            price = BigDecimal("29.99")
        )

        mockMvc.perform(
            post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.customerName").value("Integration Test User"))
            .andExpect(jsonPath("$.product").value("Test Product"))
            .andExpect(jsonPath("$.quantity").value(3))
            .andExpect(jsonPath("$.status").value("CREATED"))
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.updatedAt").exists())

        // Verify the event was published to PubSub
        val channel = createGrpcChannel()
        val channelProvider = FixedTransportChannelProvider.create(GrpcTransportChannel.create(channel))
        val credentialsProvider = NoCredentialsProvider.create()

        try {
            val subscriberStubSettings = SubscriberStubSettings.newBuilder()
                .setTransportChannelProvider(channelProvider)
                .setCredentialsProvider(credentialsProvider)
                .build()

            GrpcSubscriberStub.create(subscriberStubSettings).use { subscriber ->
                val pullRequest = PullRequest.newBuilder()
                    .setMaxMessages(10)
                    .setSubscription(ProjectSubscriptionName.format(PROJECT_ID, ORDER_EVENTS_SUBSCRIPTION_ID))
                    .build()
                val pullResponse = subscriber.pullCallable().call(pullRequest)

                assertTrue(pullResponse.receivedMessagesList.isNotEmpty(), "Expected at least one message on order-events")
                val lastMessage = pullResponse.receivedMessagesList.last()
                val payload = lastMessage.message.data.toStringUtf8()
                val eventMap = objectMapper.readValue(payload, Map::class.java)
                assertEquals("Integration Test User", eventMap["customerName"])
                assertEquals("Test Product", eventMap["product"])
                assertEquals(3, eventMap["quantity"])
                assertNotNull(eventMap["orderId"])
                assertNotNull(eventMap["timestamp"])
            }
        } finally {
            channel.shutdown().awaitTermination(5, TimeUnit.SECONDS)
        }
    }

    @Test
    fun `POST api_orders returns 400 for invalid request`() {
        val body = """{"customerName":"","product":"Widget","quantity":-1,"price":0}"""

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
    fun `GET api_orders returns all orders`() {
        createOrder("List User", "List Product", 1, BigDecimal("9.99"))

        mockMvc.perform(get("/api/orders"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)))
    }

    @Test
    fun `GET api_orders_id returns order by id`() {
        val orderId = createOrder("Get User", "Get Product", 2, BigDecimal("15.00"))

        mockMvc.perform(get("/api/orders/$orderId"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(orderId))
            .andExpect(jsonPath("$.customerName").value("Get User"))
            .andExpect(jsonPath("$.product").value("Get Product"))
    }

    @Test
    fun `GET api_orders_id returns 404 for nonexistent order`() {
        val nonExistentId = UUID.randomUUID()

        mockMvc.perform(get("/api/orders/$nonExistentId"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.error").value("Not Found"))
    }

    @Test
    fun `PUT api_orders_id updates order`() {
        val orderId = createOrder("Update User", "Old Product", 1, BigDecimal("10.00"))

        val updateRequest = UpdateOrderRequest(
            customerName = "Updated User",
            product = "New Product",
            quantity = 5,
            price = BigDecimal("25.00"),
            status = OrderStatus.CONFIRMED
        )

        mockMvc.perform(
            put("/api/orders/$orderId")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.customerName").value("Updated User"))
            .andExpect(jsonPath("$.product").value("New Product"))
            .andExpect(jsonPath("$.quantity").value(5))
            .andExpect(jsonPath("$.status").value("CONFIRMED"))
    }

    @Test
    fun `DELETE api_orders_id deletes order and returns 204`() {
        val orderId = createOrder("Delete User", "Delete Product", 1, BigDecimal("5.00"))

        mockMvc.perform(delete("/api/orders/$orderId"))
            .andExpect(status().isNoContent)

        // Verify it's gone
        mockMvc.perform(get("/api/orders/$orderId"))
            .andExpect(status().isNotFound)
    }
}
