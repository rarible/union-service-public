package com.rarible.protocol.union.dto

import com.fasterxml.jackson.databind.module.SimpleModule
import com.rarible.protocol.union.dto.ethereum.EthAddress
import com.rarible.protocol.union.dto.ethereum.deserializer.EthAddressDeserializer
import com.rarible.protocol.union.dto.ethereum.serializer.EthAddressSerializer
import com.rarible.protocol.union.dto.flow.FlowAddress
import com.rarible.protocol.union.dto.flow.FlowContract
import com.rarible.protocol.union.dto.flow.deserializer.FlowAddressDeserializer
import com.rarible.protocol.union.dto.flow.deserializer.FlowContractDeserializer
import com.rarible.protocol.union.dto.flow.serializer.FlowAddressSerializer
import com.rarible.protocol.union.dto.flow.serializer.FlowContractSerializer

object UnionPrimitivesJacksonModule : SimpleModule() {

    init {
        addSerializer(EthAddressSerializer)
        addDeserializer(EthAddress::class.java, EthAddressDeserializer)

        addSerializer(FlowAddressSerializer)
        addDeserializer(FlowAddress::class.java, FlowAddressDeserializer)

        addSerializer(FlowContractSerializer)
        addDeserializer(FlowContract::class.java, FlowContractDeserializer)
    }

}