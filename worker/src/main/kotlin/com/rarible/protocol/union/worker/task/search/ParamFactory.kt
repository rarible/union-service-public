package com.rarible.protocol.union.worker.task.search

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.stereotype.Component

@Component
class ParamFactory(
    val objectMapper: ObjectMapper
) {

    final inline fun <reified T> parse(str: String): T {
        return objectMapper.readValue(str)
    }

    fun <T> toString(obj: T): String {
        return objectMapper.writeValueAsString(obj)
    }
}