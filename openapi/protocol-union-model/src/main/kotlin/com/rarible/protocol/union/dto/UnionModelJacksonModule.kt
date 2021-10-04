package com.rarible.protocol.union.dto

import com.fasterxml.jackson.databind.module.SimpleModule
import com.rarible.protocol.union.dto.deserializer.ActivityIdDeserializer
import com.rarible.protocol.union.dto.deserializer.ItemIdDeserializer
import com.rarible.protocol.union.dto.deserializer.OrderIdDeserializer
import com.rarible.protocol.union.dto.deserializer.OwnershipIdDeserializer
import com.rarible.protocol.union.dto.serializer.ActivityIdSerializer
import com.rarible.protocol.union.dto.serializer.ItemIdSerializer
import com.rarible.protocol.union.dto.serializer.OrderIdSerializer
import com.rarible.protocol.union.dto.serializer.OwnershipIdSerializer

object UnionModelJacksonModule : SimpleModule() {

    init {
        addSerializer(ItemIdSerializer)
        addDeserializer(ItemIdDto::class.java, ItemIdDeserializer)

        addSerializer(OwnershipIdSerializer)
        addDeserializer(OwnershipIdDto::class.java, OwnershipIdDeserializer)

        addSerializer(OrderIdSerializer)
        addDeserializer(OrderIdDto::class.java, OrderIdDeserializer)

        addSerializer(ActivityIdSerializer)
        addDeserializer(ActivityIdDto::class.java, ActivityIdDeserializer)
    }

}