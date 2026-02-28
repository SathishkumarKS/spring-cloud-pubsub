package io.satkuppu.learn.spring_cloud_pubsub.order

import io.satkuppu.learn.spring_cloud_pubsub.SpringCloudPubsubApplication
import io.satkuppu.learn.spring_cloud_pubsub.order.utils.PubSubEmulator
import io.specmatic.test.SpecmaticContractTest
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.springframework.boot.SpringApplication
import org.springframework.context.ConfigurableApplicationContext

class RestContractTest : SpecmaticContractTest {

    companion object {
        private const val PROJECT_ID = "hello-world"
        private lateinit var context: ConfigurableApplicationContext
        private val pubSubEmulator = PubSubEmulator(projectId = PROJECT_ID)



        @JvmStatic
        @BeforeAll
        fun setUp() {
            System.setProperty("host", "localhost")
            System.setProperty("port", "8090")
            pubSubEmulator.start()
            context = SpringApplication.run(
                SpringCloudPubsubApplication::class.java,
                "--server.port=8090",
                "--spring.cloud.gcp.project-id=$PROJECT_ID",
                "--spring.cloud.gcp.pubsub.emulator-host=localhost:8085"
            )
        }

        @JvmStatic
        @AfterAll
        fun tearDown() {
            if (::context.isInitialized) {
                context.close()
            }
            pubSubEmulator.stop()
        }
    }
}
