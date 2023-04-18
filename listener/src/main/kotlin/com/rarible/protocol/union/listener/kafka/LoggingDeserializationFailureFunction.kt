package com.rarible.protocol.union.listener.kafka

import org.slf4j.LoggerFactory
import org.springframework.kafka.support.serializer.FailedDeserializationInfo
import java.util.function.Function

class LoggingDeserializationFailureFunction : Function<FailedDeserializationInfo, Any?> {
    override fun apply(t: FailedDeserializationInfo): Any? {
        logger.error("Failed to deserialize ${String(t.data)} from topic: ${t.topic}", t.exception)
        return null
    }

    companion object {
        private val logger = LoggerFactory.getLogger(LoggingDeserializationFailureFunction::class.java)
    }
}
