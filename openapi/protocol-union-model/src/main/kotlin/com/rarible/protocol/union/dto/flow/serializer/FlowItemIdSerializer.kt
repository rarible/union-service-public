package com.rarible.protocol.union.dto.flow.serializer

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import com.rarible.protocol.union.dto.flow.FlowItemIdDto

object FlowItemIdSerializer : StdSerializer<FlowItemIdDto>(FlowItemIdDto::class.java) {

    override fun serialize(id: FlowItemIdDto?, gen: JsonGenerator, provider: SerializerProvider) {
        if (id == null) {
            gen.writeNull()
            return
        }
        gen.writeStartObject()
        gen.writeStringField(FlowItemIdDto::value.name, id.fullId())
        provider.defaultSerializeField(FlowItemIdDto::blockchain.name, id.blockchain, gen)
        provider.defaultSerializeField(FlowItemIdDto::token.name, id.token, gen)
        provider.defaultSerializeField(FlowItemIdDto::tokenId.name, id.tokenId, gen)
        gen.writeEndObject();
    }
}