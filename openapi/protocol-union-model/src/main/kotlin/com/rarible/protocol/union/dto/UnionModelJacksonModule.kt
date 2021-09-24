package com.rarible.protocol.union.dto

import com.fasterxml.jackson.databind.module.SimpleModule
import com.rarible.protocol.union.dto.deserializer.UnionActivityIdDeserializer
import com.rarible.protocol.union.dto.deserializer.UnionItemIdDeserializer
import com.rarible.protocol.union.dto.deserializer.UnionOrderIdDeserializer
import com.rarible.protocol.union.dto.deserializer.UnionOwnershipIdDeserializer
import com.rarible.protocol.union.dto.serializer.UnionActivityIdSerializer
import com.rarible.protocol.union.dto.serializer.UnionItemIdSerializer
import com.rarible.protocol.union.dto.serializer.UnionOrderIdSerializer
import com.rarible.protocol.union.dto.serializer.UnionOwnershipIdSerializer

object UnionModelJacksonModule : SimpleModule() {

    init {
        addSerializer(UnionItemIdSerializer)
        addDeserializer(UnionItemIdDto::class.java, UnionItemIdDeserializer)

        addSerializer(UnionOwnershipIdSerializer)
        addDeserializer(UnionOwnershipIdDto::class.java, UnionOwnershipIdDeserializer)

        addSerializer(UnionOrderIdSerializer)
        addDeserializer(UnionOrderIdDto::class.java, UnionOrderIdDeserializer)

        addSerializer(UnionActivityIdSerializer)
        addDeserializer(UnionActivityIdDto::class.java, UnionActivityIdDeserializer)
    }

}