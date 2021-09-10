package com.rarible.protocol.union.dto.flow.deserializer

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.rarible.protocol.union.dto.FlowBlockchainDto
import com.rarible.protocol.union.dto.FlowOrderIdDto
import com.rarible.protocol.union.dto.IdParser

object FlowOrderIdDeserializer : StdDeserializer<FlowOrderIdDto>(FlowOrderIdDto::class.java) {

    override fun deserialize(p: JsonParser, ctxt: DeserializationContext?): FlowOrderIdDto? {
        val value = p.codec.readValue(p, String::class.java) ?: return null
        val pair = IdParser.parse(value)
        return FlowOrderIdDto(pair.second, FlowBlockchainDto.valueOf(pair.first.name))
    }
}