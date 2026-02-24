package io.satkuppu.learn.spring_cloud_pubsub

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class HelloController(private val helloPubSubService: HelloPubSubService) {

    @GetMapping("/hello")
    fun hello(): Map<String, String> {
        val result = helloPubSubService.sendHelloMessage()
        return mapOf("status" to result)
    }
}
