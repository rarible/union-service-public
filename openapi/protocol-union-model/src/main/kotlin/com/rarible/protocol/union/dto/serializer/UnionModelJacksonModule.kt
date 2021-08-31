package com.rarible.protocol.union.dto.serializer

import com.fasterxml.jackson.databind.module.SimpleModule
import com.rarible.protocol.union.dto.EthItemIdDto
import com.rarible.protocol.union.dto.EthOwnershipIdDto
import com.rarible.protocol.union.dto.FlowItemIdDto
import com.rarible.protocol.union.dto.FlowOwnershipIdDto
import com.rarible.protocol.union.dto.serializer.eth.EthItemIdDeserializer
import com.rarible.protocol.union.dto.serializer.eth.EthItemIdSerializer
import com.rarible.protocol.union.dto.serializer.eth.EthOwnershipIdDeserializer
import com.rarible.protocol.union.dto.serializer.eth.EthOwnershipIdSerializer
import com.rarible.protocol.union.dto.serializer.flow.FlowItemIdDeserializer
import com.rarible.protocol.union.dto.serializer.flow.FlowItemIdSerializer
import com.rarible.protocol.union.dto.serializer.flow.FlowOwnershipIdDeserializer
import com.rarible.protocol.union.dto.serializer.flow.FlowOwnershipIdSerializer

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