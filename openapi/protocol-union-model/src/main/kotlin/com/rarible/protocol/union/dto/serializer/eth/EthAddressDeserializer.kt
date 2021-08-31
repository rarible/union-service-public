package com.rarible.protocol.union.dto.serializer.eth

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.EthAddress
import com.rarible.protocol.union.dto.serializer.IdParser

object EthAddressDeserializer : StdDeserializer<EthAddress>(EthAddress::class.java) {

    override fun deserialize(p: JsonParser, ctxt: DeserializationContext?): EthAddress? {
        val value = p.codec.readValue(p, String::class.java) ?: return null
        return EthAddress(IdParser.parse(value, BlockchainDto.ETHEREUM).second)

    }
}