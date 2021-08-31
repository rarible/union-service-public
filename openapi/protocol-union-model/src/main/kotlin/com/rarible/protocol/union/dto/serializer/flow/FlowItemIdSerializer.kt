package com.rarible.protocol.union.dto.serializer.flow

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.FlowItemIdDto

object FlowItemIdSerializer : StdSerializer<FlowItemIdDto>(FlowItemIdDto::class.java) {

    override fun serialize(id: FlowItemIdDto?, gen: JsonGenerator, provider: SerializerProvider) {
        if (id == null) {
            gen.writeNull()
            return
        }
        gen.writeStartObject()
        gen.writeStringField(FlowItemIdDto::value.name, "${BlockchainDto.FLOW}:${id.value}");
        provider.defaultSerializeField(FlowItemIdDto::token.name, id.token, gen)
        provider.defaultSerializeField(FlowItemIdDto::tokenId.name, id.tokenId, gen)
        gen.writeEndObject();
    }
}