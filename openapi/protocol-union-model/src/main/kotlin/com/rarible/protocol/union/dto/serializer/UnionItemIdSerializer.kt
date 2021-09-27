package com.rarible.protocol.union.dto.serializer

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import com.rarible.protocol.union.dto.UnionItemIdDto

object UnionItemIdSerializer : StdSerializer<UnionItemIdDto>(UnionItemIdDto::class.java) {

    override fun serialize(id: UnionItemIdDto?, gen: JsonGenerator, provider: SerializerProvider) {
        if (id == null) {
            gen.writeNull()
            return
        }
        gen.writeStartObject()
        gen.writeStringField(UnionItemIdDto::value.name, id.fullId())
        provider.defaultSerializeField(UnionItemIdDto::blockchain.name, id.blockchain, gen)
        provider.defaultSerializeField(UnionItemIdDto::token.name, id.token, gen)
        provider.defaultSerializeField(UnionItemIdDto::tokenId.name, id.tokenId, gen)
        gen.writeEndObject()
    }
}