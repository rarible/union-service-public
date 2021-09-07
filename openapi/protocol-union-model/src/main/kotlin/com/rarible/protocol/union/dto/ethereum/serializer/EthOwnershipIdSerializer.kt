package com.rarible.protocol.union.dto.ethereum.serializer

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import com.rarible.protocol.union.dto.EthOwnershipIdDto

object EthOwnershipIdSerializer : StdSerializer<EthOwnershipIdDto>(EthOwnershipIdDto::class.java) {

    override fun serialize(id: EthOwnershipIdDto?, gen: JsonGenerator, provider: SerializerProvider) {
        if (id == null) {
            gen.writeNull()
            return
        }
        gen.writeStartObject()
        gen.writeStringField(EthOwnershipIdDto::value.name, "${id.blockchain.name}:${id.value}")
        provider.defaultSerializeField(EthOwnershipIdDto::blockchain.name, id.blockchain, gen)
        provider.defaultSerializeField(EthOwnershipIdDto::token.name, id.token, gen)
        provider.defaultSerializeField(EthOwnershipIdDto::tokenId.name, id.tokenId, gen)
        provider.defaultSerializeField(EthOwnershipIdDto::owner.name, id.owner, gen)
        gen.writeEndObject()
    }
}