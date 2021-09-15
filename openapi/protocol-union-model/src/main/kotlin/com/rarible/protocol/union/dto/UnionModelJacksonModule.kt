package com.rarible.protocol.union.dto

import com.fasterxml.jackson.databind.module.SimpleModule
import com.rarible.protocol.union.dto.ethereum.EthActivityIdDto
import com.rarible.protocol.union.dto.ethereum.EthItemIdDto
import com.rarible.protocol.union.dto.ethereum.EthOrderIdDto
import com.rarible.protocol.union.dto.ethereum.EthOwnershipIdDto
import com.rarible.protocol.union.dto.ethereum.deserializer.EthActivityIdDeserializer
import com.rarible.protocol.union.dto.ethereum.deserializer.EthItemIdDeserializer
import com.rarible.protocol.union.dto.ethereum.deserializer.EthOrderIdDeserializer
import com.rarible.protocol.union.dto.ethereum.deserializer.EthOwnershipIdDeserializer
import com.rarible.protocol.union.dto.ethereum.serializer.EthActivityIdSerializer
import com.rarible.protocol.union.dto.ethereum.serializer.EthItemIdSerializer
import com.rarible.protocol.union.dto.ethereum.serializer.EthOrderIdSerializer
import com.rarible.protocol.union.dto.ethereum.serializer.EthOwnershipIdSerializer
import com.rarible.protocol.union.dto.flow.FlowActivityIdDto
import com.rarible.protocol.union.dto.flow.FlowItemIdDto
import com.rarible.protocol.union.dto.flow.FlowOrderIdDto
import com.rarible.protocol.union.dto.flow.FlowOwnershipIdDto
import com.rarible.protocol.union.dto.flow.deserializer.FlowActivityIdDeserializer
import com.rarible.protocol.union.dto.flow.deserializer.FlowItemIdDeserializer
import com.rarible.protocol.union.dto.flow.deserializer.FlowOrderIdDeserializer
import com.rarible.protocol.union.dto.flow.deserializer.FlowOwnershipIdDeserializer
import com.rarible.protocol.union.dto.flow.serializer.FlowActivityIdSerializer
import com.rarible.protocol.union.dto.flow.serializer.FlowItemIdSerializer
import com.rarible.protocol.union.dto.flow.serializer.FlowOrderIdSerializer
import com.rarible.protocol.union.dto.flow.serializer.FlowOwnershipIdSerializer

object UnionModelJacksonModule : SimpleModule() {

    init {
        addSerializer(EthItemIdSerializer)
        addDeserializer(EthItemIdDto::class.java, EthItemIdDeserializer)

        addSerializer(EthOwnershipIdSerializer)
        addDeserializer(EthOwnershipIdDto::class.java, EthOwnershipIdDeserializer)

        addSerializer(EthOrderIdSerializer)
        addDeserializer(EthOrderIdDto::class.java, EthOrderIdDeserializer)

        addSerializer(EthActivityIdSerializer)
        addDeserializer(EthActivityIdDto::class.java, EthActivityIdDeserializer)

        addSerializer(FlowItemIdSerializer)
        addDeserializer(FlowItemIdDto::class.java, FlowItemIdDeserializer)

        addSerializer(FlowOwnershipIdSerializer)
        addDeserializer(FlowOwnershipIdDto::class.java, FlowOwnershipIdDeserializer)

        addSerializer(FlowOrderIdSerializer)
        addDeserializer(FlowOrderIdDto::class.java, FlowOrderIdDeserializer)

        addSerializer(FlowActivityIdSerializer)
        addDeserializer(FlowActivityIdDto::class.java, FlowActivityIdDeserializer)
    }

}