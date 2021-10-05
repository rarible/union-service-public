package com.rarible.protocol.union.dto.deserializer

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.parser.ItemIdParser

object ItemIdDeserializer : StdDeserializer<ItemIdDto>(ItemIdDto::class.java) {

    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): ItemIdDto? {
        val value = p.codec.readValue(p, String::class.java) ?: return null
        return ItemIdParser.parseFull(value)
    }
}