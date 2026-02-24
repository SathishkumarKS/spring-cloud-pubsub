package io.satkuppu.learn.spring_cloud_pubsub

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class SpringCloudPubsubApplication

fun main(args: Array<String>) {
	runApplication<SpringCloudPubsubApplication>(*args)
}
