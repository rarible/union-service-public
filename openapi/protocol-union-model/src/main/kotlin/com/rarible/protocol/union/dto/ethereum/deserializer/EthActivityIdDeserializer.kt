package com.rarible.protocol.union.dto.ethereum.deserializer

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.rarible.protocol.union.dto.EthActivityIdDto
import com.rarible.protocol.union.dto.EthBlockchainDto
import com.rarible.protocol.union.dto.IdParser

object EthActivityIdDeserializer : StdDeserializer<EthActivityIdDto>(EthActivityIdDto::class.java) {

    override fun deserialize(p: JsonParser, ctxt: DeserializationContext?): EthActivityIdDto? {
        val value = p.codec.readValue(p, String::class.java) ?: return null
        val pair = IdParser.parse(value)
        return EthActivityIdDto(pair.second, EthBlockchainDto.valueOf(pair.first.name))
    }
}