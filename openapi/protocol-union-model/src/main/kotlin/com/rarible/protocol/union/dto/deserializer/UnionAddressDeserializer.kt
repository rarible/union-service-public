package com.rarible.protocol.union.dto.deserializer

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.rarible.protocol.union.dto.UnionAddress
import com.rarible.protocol.union.dto.parser.IdParser

object UnionAddressDeserializer : StdDeserializer<UnionAddress>(UnionAddress::class.java) {

    override fun deserialize(p: JsonParser, ctxt: DeserializationContext?): UnionAddress? {
        val value = p.codec.readValue(p, String::class.java) ?: return null
        return IdParser.parseAddress(value)
    }
}