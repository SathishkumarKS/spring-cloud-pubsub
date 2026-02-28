package io.satkuppu.learn.spring_cloud_pubsub.order

import io.satkuppu.learn.spring_cloud_pubsub.SpringCloudPubsubApplication
import io.satkuppu.learn.spring_cloud_pubsub.order.utils.PubSubEmulator
import io.specmatic.googlepubsub.mock.GooglePubSubMock
import io.specmatic.googlepubsub.mock.SpecmaticGooglePubSubTestBase
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.springframework.boot.runApplication
import org.springframework.context.ConfigurableApplicationContext

class PubSubContractTest : SpecmaticGooglePubSubTestBase() {

    companion object {
        private const val PROJECT_ID = "hello-world"
        private const val SHUTDOWN_TIMEOUT_FOR_MOCK_IN_MS = 1000
        private lateinit var context: ConfigurableApplicationContext

        private val pubSubEmulator = PubSubEmulator(projectId = PROJECT_ID)

        @JvmStatic
        @BeforeAll
        fun setUp() {
            System.setProperty("OVERLAY_FILE", "src/test/resources/spec_overlay.yaml")
            pubSubEmulator.start()
            googlePubSubMock = GooglePubSubMock.connectWithBroker(PROJECT_ID)
            context = runApplication<SpringCloudPubsubApplication>(
                "--server.port=8090",
                "--spring.cloud.stream.default-binder=pubsub",
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
            googlePubSubMock.stop(SHUTDOWN_TIMEOUT_FOR_MOCK_IN_MS)
            pubSubEmulator.stop()
        }
    }
}
