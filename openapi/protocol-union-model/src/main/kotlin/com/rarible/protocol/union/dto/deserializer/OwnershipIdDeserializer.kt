package com.rarible.protocol.union.dto.deserializer

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.rarible.protocol.union.dto.OwnershipIdDto
import com.rarible.protocol.union.dto.parser.OwnershipIdParser

object OwnershipIdDeserializer : StdDeserializer<OwnershipIdDto>(OwnershipIdDto::class.java) {

    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): OwnershipIdDto? {
        val value = p.codec.readValue(p, String::class.java) ?: return null
        return OwnershipIdParser.parseFull(value)
    }
}