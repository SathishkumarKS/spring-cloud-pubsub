package io.satkuppu.learn.spring_cloud_pubsub

import org.slf4j.LoggerFactory
import org.springframework.cloud.stream.function.StreamBridge
import org.springframework.stereotype.Service

@Service
class HelloPubSubService(private val streamBridge: StreamBridge) {

    private val logger = LoggerFactory.getLogger(HelloPubSubService::class.java)

    fun sendHelloMessage(): String {
        val message = "Hello PubSub"
        val sent = streamBridge.send("hello-out-0", message)
        logger.info("Message sent to hello-out-0: success={}", sent)
        return if (sent) "Message sent successfully" else "Failed to send message"
    }
}
