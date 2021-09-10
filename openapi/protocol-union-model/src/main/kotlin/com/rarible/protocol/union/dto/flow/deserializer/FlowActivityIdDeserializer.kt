package com.rarible.protocol.union.dto.flow.deserializer

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.rarible.protocol.union.dto.FlowActivityIdDto
import com.rarible.protocol.union.dto.FlowBlockchainDto
import com.rarible.protocol.union.dto.IdParser

object FlowActivityIdDeserializer : StdDeserializer<FlowActivityIdDto>(FlowActivityIdDto::class.java) {

    override fun deserialize(p: JsonParser, ctxt: DeserializationContext?): FlowActivityIdDto? {
        val value = p.codec.readValue(p, String::class.java) ?: return null
        val pair = IdParser.parse(value)
        return FlowActivityIdDto(pair.second, FlowBlockchainDto.valueOf(pair.first.name))
    }
}