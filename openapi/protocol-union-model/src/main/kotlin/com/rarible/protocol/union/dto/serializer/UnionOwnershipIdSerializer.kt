package com.rarible.protocol.union.dto.serializer

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import com.rarible.protocol.union.dto.UnionOwnershipIdDto

object UnionOwnershipIdSerializer : StdSerializer<UnionOwnershipIdDto>(UnionOwnershipIdDto::class.java) {

    override fun serialize(id: UnionOwnershipIdDto?, gen: JsonGenerator, provider: SerializerProvider) {
        if (id == null) {
            gen.writeNull()
            return
        }
        gen.writeStartObject()
        gen.writeStringField(UnionOwnershipIdDto::value.name, id.fullId())
        provider.defaultSerializeField(UnionOwnershipIdDto::blockchain.name, id.blockchain, gen)
        provider.defaultSerializeField(UnionOwnershipIdDto::token.name, id.token, gen)
        provider.defaultSerializeField(UnionOwnershipIdDto::tokenId.name, id.tokenId, gen)
        provider.defaultSerializeField(UnionOwnershipIdDto::owner.name, id.owner, gen)
        gen.writeEndObject()
    }
}