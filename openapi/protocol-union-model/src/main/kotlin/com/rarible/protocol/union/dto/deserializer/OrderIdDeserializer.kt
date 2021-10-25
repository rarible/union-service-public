package com.rarible.protocol.union.dto.deserializer

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.rarible.protocol.union.dto.OrderIdDto
import com.rarible.protocol.union.dto.parser.IdParser

object OrderIdDeserializer : StdDeserializer<OrderIdDto>(OrderIdDto::class.java) {

    override fun deserialize(p: JsonParser, ctxt: DeserializationContext?): OrderIdDto? {
        val value = p.codec.readValue(p, String::class.java) ?: return null
        return IdParser.parseOrderId(value)
    }
}