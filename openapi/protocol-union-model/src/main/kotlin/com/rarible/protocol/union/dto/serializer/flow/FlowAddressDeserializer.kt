package com.rarible.protocol.union.dto.serializer.flow

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.FlowAddress
import com.rarible.protocol.union.dto.serializer.IdParser

object FlowAddressDeserializer : StdDeserializer<FlowAddress>(FlowAddress::class.java) {

    override fun deserialize(p: JsonParser, ctxt: DeserializationContext?): FlowAddress? {
        val value = p.codec.readValue(p, String::class.java) ?: return null
        return FlowAddress(IdParser.parse(value, BlockchainDto.FLOW).second)

    }
}