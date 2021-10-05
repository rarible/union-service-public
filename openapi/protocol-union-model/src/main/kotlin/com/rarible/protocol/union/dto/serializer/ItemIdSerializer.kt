package com.rarible.protocol.union.dto.serializer

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import com.rarible.protocol.union.dto.ItemIdDto

object ItemIdSerializer : StdSerializer<ItemIdDto>(ItemIdDto::class.java) {

    override fun serialize(id: ItemIdDto?, gen: JsonGenerator, provider: SerializerProvider) {
        if (id == null) {
            gen.writeNull()
            return
        }
        gen.writeString(id.fullId())
    }
}