package com.rarible.protocol.union.dto.ethereum.deserializer

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.rarible.protocol.union.dto.EthBlockchainDto
import com.rarible.protocol.union.dto.IdParser
import com.rarible.protocol.union.dto.ethereum.EthAddress

object EthAddressDeserializer : StdDeserializer<EthAddress>(EthAddress::class.java) {

    override fun deserialize(p: JsonParser, ctxt: DeserializationContext?): EthAddress? {
        val value = p.codec.readValue(p, String::class.java) ?: return null
        val pair = IdParser.parse(value)
        return EthAddress(EthBlockchainDto.valueOf(pair.first.name), pair.second)

    }
}