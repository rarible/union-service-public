package com.rarible.protocol.union.dto.ethereum.serializer

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import com.rarible.protocol.union.dto.EthItemIdDto

object EthItemIdSerializer : StdSerializer<EthItemIdDto>(EthItemIdDto::class.java) {

    override fun serialize(id: EthItemIdDto?, gen: JsonGenerator, provider: SerializerProvider) {
        if (id == null) {
            gen.writeNull()
            return
        }
        gen.writeStartObject()
        gen.writeStringField(EthItemIdDto::value.name, "${id.blockchain.name}:${id.value}")
        provider.defaultSerializeField(EthItemIdDto::blockchain.name, id.blockchain, gen)
        provider.defaultSerializeField(EthItemIdDto::token.name, id.token, gen)
        provider.defaultSerializeField(EthItemIdDto::tokenId.name, id.tokenId, gen)
        gen.writeEndObject();
    }
}