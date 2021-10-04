package com.rarible.protocol.union.dto.serializer

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import com.rarible.protocol.union.dto.OwnershipIdDto

object OwnershipIdSerializer : StdSerializer<OwnershipIdDto>(OwnershipIdDto::class.java) {

    override fun serialize(id: OwnershipIdDto?, gen: JsonGenerator, provider: SerializerProvider) {
        if (id == null) {
            gen.writeNull()
            return
        }
        gen.writeStartObject()
        gen.writeStringField(OwnershipIdDto::value.name, id.fullId())
        provider.defaultSerializeField(OwnershipIdDto::blockchain.name, id.blockchain, gen)
        provider.defaultSerializeField(OwnershipIdDto::token.name, id.token, gen)
        provider.defaultSerializeField(OwnershipIdDto::tokenId.name, id.tokenId, gen)
        provider.defaultSerializeField(OwnershipIdDto::owner.name, id.owner, gen)
        gen.writeEndObject()
    }
}