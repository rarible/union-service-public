package com.rarible.protocol.union.dto.flow.serializer

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import com.rarible.protocol.union.dto.FlowOrderIdDto

object FlowOrderIdSerializer : StdSerializer<FlowOrderIdDto>(FlowOrderIdDto::class.java) {

    override fun serialize(value: FlowOrderIdDto?, gen: JsonGenerator, provider: SerializerProvider?) {
        if (value == null) {
            gen.writeNull()
            return
        }
        gen.writeString("${value.blockchain.name}:${value.value}")
    }
}