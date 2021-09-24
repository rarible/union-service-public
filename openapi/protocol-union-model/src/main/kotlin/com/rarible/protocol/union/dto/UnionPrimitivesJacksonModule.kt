package com.rarible.protocol.union.dto

import com.fasterxml.jackson.databind.module.SimpleModule
import com.rarible.protocol.union.dto.deserializer.UnionAddressDeserializer
import com.rarible.protocol.union.dto.serializer.UnionAddressSerializer

object UnionPrimitivesJacksonModule : SimpleModule() {

    init {
        addSerializer(UnionAddressSerializer)
        addDeserializer(UnionAddress::class.java, UnionAddressDeserializer)
    }

}