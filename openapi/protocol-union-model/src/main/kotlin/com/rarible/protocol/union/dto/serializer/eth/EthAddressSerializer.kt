package com.rarible.protocol.union.dto.serializer.eth

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.EthAddress

object EthAddressSerializer : StdSerializer<EthAddress>(EthAddress::class.java) {

    override fun serialize(value: EthAddress?, gen: JsonGenerator, provider: SerializerProvider?) {
        if (value == null) {
            gen.writeNull()
            return
        }
        gen.writeString("${BlockchainDto.ETHEREUM}:${value.value}")
    }
}