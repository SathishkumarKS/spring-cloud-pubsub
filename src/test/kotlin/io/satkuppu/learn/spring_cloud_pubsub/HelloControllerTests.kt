package io.satkuppu.learn.spring_cloud_pubsub

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.cloud.stream.binder.test.OutputDestination
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration
import org.springframework.context.annotation.Import
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestChannelBinderConfiguration::class)
class HelloControllerTests {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var outputDestination: OutputDestination

    @Test
    fun `GET hello sends message to pubsub`() {
        mockMvc.perform(get("/hello"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("Message sent successfully"))

        val message = outputDestination.receive(1000, "hello-topic")
        assertNotNull(message)
        val payload = String(message.payload)
        assert(payload.contains("Hello PubSub")) { "Expected payload to contain 'Hello PubSub' but was: $payload" }
    }
}
