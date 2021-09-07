package com.rarible.protocol.union.dto.flow.deserializer

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.rarible.protocol.union.dto.FlowBlockchainDto
import com.rarible.protocol.union.dto.IdParser
import com.rarible.protocol.union.dto.flow.FlowAddress

object FlowAddressDeserializer : StdDeserializer<FlowAddress>(FlowAddress::class.java) {

    override fun deserialize(p: JsonParser, ctxt: DeserializationContext?): FlowAddress? {
        val value = p.codec.readValue(p, String::class.java) ?: return null
        val pair = IdParser.parse(value)
        return FlowAddress(FlowBlockchainDto.valueOf(pair.first), pair.second)

    }
}