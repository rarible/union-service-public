package com.rarible.protocol.union.dto

import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.ser.std.NumberSerializer
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer
import com.rarible.protocol.union.dto.deserializer.UnionAddressDeserializer
import com.rarible.protocol.union.dto.serializer.UnionAddressSerializer
import java.math.BigInteger

object UnionPrimitivesJacksonModule : SimpleModule() {

    init {
        addSerializer(UnionAddressSerializer)
        addDeserializer(UnionAddress::class.java, UnionAddressDeserializer)

        addSerializer(NumberSerializer.bigDecimalAsStringSerializer())
        addSerializer(ToStringSerializer(BigInteger::class.java))
    }

}