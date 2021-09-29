package com.rarible.protocol.union.dto.deserializer

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.rarible.protocol.union.dto.ActivityIdDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.IdParser

object ActivityIdDeserializer : StdDeserializer<ActivityIdDto>(ActivityIdDto::class.java) {

    override fun deserialize(p: JsonParser, ctxt: DeserializationContext?): ActivityIdDto? {
        val value = p.codec.readValue(p, String::class.java) ?: return null
        val pair = IdParser.parse(value)
        return ActivityIdDto(BlockchainDto.valueOf(pair.first.name), pair.second)
    }
}