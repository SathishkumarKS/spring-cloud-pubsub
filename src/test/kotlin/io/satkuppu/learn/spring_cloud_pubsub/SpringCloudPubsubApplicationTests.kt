package io.satkuppu.learn.spring_cloud_pubsub

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import

@Import(TestcontainersConfiguration::class)
@SpringBootTest
class SpringCloudPubsubApplicationTests {

	@Test
	fun contextLoads() {
	}

}
