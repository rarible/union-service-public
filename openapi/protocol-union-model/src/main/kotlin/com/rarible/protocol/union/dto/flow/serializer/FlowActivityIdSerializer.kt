package com.rarible.protocol.union.dto.flow.serializer

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import com.rarible.protocol.union.dto.FlowActivityIdDto

object FlowActivityIdSerializer : StdSerializer<FlowActivityIdDto>(FlowActivityIdDto::class.java) {

    override fun serialize(value: FlowActivityIdDto?, gen: JsonGenerator, provider: SerializerProvider?) {
        if (value == null) {
            gen.writeNull()
            return
        }
        gen.writeString("${value.blockchain.name}:${value.value}")
    }
}