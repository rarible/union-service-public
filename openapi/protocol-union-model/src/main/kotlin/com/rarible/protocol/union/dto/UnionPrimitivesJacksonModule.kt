package com.rarible.protocol.union.dto

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.ser.std.StdScalarSerializer
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer
import com.rarible.protocol.union.dto.deserializer.UnionAddressDeserializer
import com.rarible.protocol.union.dto.serializer.UnionAddressSerializer
import java.math.BigDecimal
import java.math.BigInteger

object UnionPrimitivesJacksonModule : SimpleModule() {

    init {
        addSerializer(UnionAddressSerializer)
        addDeserializer(UnionAddress::class.java, UnionAddressDeserializer)

        addSerializer(BigDecimalSerializer)
        addSerializer(ToStringSerializer(BigInteger::class.java))
    }

    private object BigDecimalSerializer : StdScalarSerializer<BigDecimal>(BigDecimal::class.java) {
        override fun serialize(value: BigDecimal, gen: JsonGenerator, provider: SerializerProvider) {
            gen.writeString(value.stripTrailingZeros().toPlainString())
        }
    }

}