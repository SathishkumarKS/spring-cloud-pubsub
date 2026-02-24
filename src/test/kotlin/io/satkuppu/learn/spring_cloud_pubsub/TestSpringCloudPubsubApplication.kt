package io.satkuppu.learn.spring_cloud_pubsub

import org.springframework.boot.fromApplication
import org.springframework.boot.with


fun main(args: Array<String>) {
	fromApplication<SpringCloudPubsubApplication>().with(TestcontainersConfiguration::class).run(*args)
}
