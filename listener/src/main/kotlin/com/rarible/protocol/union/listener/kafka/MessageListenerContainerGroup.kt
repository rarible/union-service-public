package com.rarible.protocol.union.listener.kafka

import com.rarible.protocol.union.core.handler.KafkaConsumerWorker
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.ApplicationEventPublisherAware
import org.springframework.kafka.listener.AbstractMessageListenerContainer

class MessageListenerContainerGroup<K, V>(
    private val containers: List<AbstractMessageListenerContainer<K, V>>
) : KafkaConsumerWorker<V>, ApplicationEventPublisherAware, ApplicationContextAware {
    override fun start() {
        containers.forEach(AbstractMessageListenerContainer<K, V>::start)
    }

    override fun close() {
        containers.forEach(AbstractMessageListenerContainer<K, V>::start)
    }

    override fun setApplicationEventPublisher(applicationEventPublisher: ApplicationEventPublisher) {
        containers.forEach {
            it.setApplicationEventPublisher(applicationEventPublisher)
        }
    }

    override fun setApplicationContext(applicationContext: ApplicationContext) {
        containers.forEach {
            it.setApplicationContext(applicationContext)
        }
    }
}