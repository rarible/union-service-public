package com.rarible.protocol.union.listener.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.rarible.core.kafka.json.JsonDeserializer
import com.rarible.protocol.union.dto.serializer.UnionModelJacksonModule
import com.rarible.protocol.union.dto.serializer.UnionPrimitivesJacksonModule

class UnionKafkaJsonDeserializer : JsonDeserializer() {

    override fun createMapper(): ObjectMapper {
        return super.createMapper()
            .registerModule(UnionPrimitivesJacksonModule)
            .registerModule(UnionModelJacksonModule)
    }
}