package com.rarible.protocol.union.dto

import com.fasterxml.jackson.databind.module.SimpleModule
import com.rarible.protocol.union.dto.ethereum.deserializer.EthItemIdDeserializer
import com.rarible.protocol.union.dto.ethereum.deserializer.EthOwnershipIdDeserializer
import com.rarible.protocol.union.dto.ethereum.serializer.EthItemIdSerializer
import com.rarible.protocol.union.dto.ethereum.serializer.EthOwnershipIdSerializer
import com.rarible.protocol.union.dto.flow.deserializer.FlowItemIdDeserializer
import com.rarible.protocol.union.dto.flow.deserializer.FlowOwnershipIdDeserializer
import com.rarible.protocol.union.dto.flow.serializer.FlowItemIdSerializer
import com.rarible.protocol.union.dto.flow.serializer.FlowOwnershipIdSerializer

object UnionModelJacksonModule : SimpleModule() {

    init {
        addSerializer(EthItemIdSerializer)
        addDeserializer(EthItemIdDto::class.java, EthItemIdDeserializer)

        addSerializer(EthOwnershipIdSerializer)
        addDeserializer(EthOwnershipIdDto::class.java, EthOwnershipIdDeserializer)

        addSerializer(FlowItemIdSerializer)
        addDeserializer(FlowItemIdDto::class.java, FlowItemIdDeserializer)

        addSerializer(FlowOwnershipIdSerializer)
        addDeserializer(FlowOwnershipIdDto::class.java, FlowOwnershipIdDeserializer)
    }

}