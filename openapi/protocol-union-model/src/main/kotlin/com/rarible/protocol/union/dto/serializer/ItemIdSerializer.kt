package com.rarible.protocol.union.dto.serializer

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import com.rarible.protocol.union.dto.ItemIdDto

object ItemIdSerializer : StdSerializer<ItemIdDto>(ItemIdDto::class.java) {

    override fun serialize(id: ItemIdDto?, gen: JsonGenerator, provider: SerializerProvider) {
        if (id == null) {
            gen.writeNull()
            return
        }
        gen.writeStartObject()
        gen.writeStringField(ItemIdDto::value.name, id.fullId())
        provider.defaultSerializeField(ItemIdDto::blockchain.name, id.blockchain, gen)
        provider.defaultSerializeField(ItemIdDto::token.name, id.token, gen)
        provider.defaultSerializeField(ItemIdDto::tokenId.name, id.tokenId, gen)
        gen.writeEndObject()
    }
}