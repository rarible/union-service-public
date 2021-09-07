package com.rarible.protocol.union.subscriber

import com.fasterxml.jackson.databind.ObjectMapper
import com.rarible.core.kafka.json.JsonDeserializer
import com.rarible.protocol.union.dto.UnionModelJacksonModule
import com.rarible.protocol.union.dto.UnionPrimitivesJacksonModule

class UnionKafkaJsonDeserializer : JsonDeserializer() {

    override fun createMapper(): ObjectMapper {
        return super.createMapper()
            .registerModule(UnionPrimitivesJacksonModule)
            .registerModule(UnionModelJacksonModule)
    }
}