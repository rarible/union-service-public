package com.rarible.protocol.union.dto.ethereum.serializer

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import com.rarible.protocol.union.dto.ethereum.EthAddress

object EthAddressSerializer : StdSerializer<EthAddress>(EthAddress::class.java) {

    override fun serialize(value: EthAddress?, gen: JsonGenerator, provider: SerializerProvider?) {
        if (value == null) {
            gen.writeNull()
            return
        }
        gen.writeString(value.fullId())
    }
}