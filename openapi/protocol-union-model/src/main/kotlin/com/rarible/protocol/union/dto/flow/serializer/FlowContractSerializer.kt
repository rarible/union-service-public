package com.rarible.protocol.union.dto.flow.serializer

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import com.rarible.protocol.union.dto.flow.FlowContract

object FlowContractSerializer : StdSerializer<FlowContract>(FlowContract::class.java) {

    override fun serialize(value: FlowContract?, gen: JsonGenerator, provider: SerializerProvider?) {
        if (value == null) {
            gen.writeNull()
            return
        }
        gen.writeString(value.fullId())
    }
}