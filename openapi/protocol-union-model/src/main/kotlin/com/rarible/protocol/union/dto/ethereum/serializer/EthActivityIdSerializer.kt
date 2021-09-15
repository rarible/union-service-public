package com.rarible.protocol.union.dto.ethereum.serializer

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import com.rarible.protocol.union.dto.ethereum.EthActivityIdDto

object EthActivityIdSerializer : StdSerializer<EthActivityIdDto>(EthActivityIdDto::class.java) {

    override fun serialize(value: EthActivityIdDto?, gen: JsonGenerator, provider: SerializerProvider?) {
        if (value == null) {
            gen.writeNull()
            return
        }
        gen.writeString(value.fullId())
    }
}