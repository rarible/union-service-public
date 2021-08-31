package com.rarible.protocol.union.dto.serializer

import com.fasterxml.jackson.databind.module.SimpleModule
import com.rarible.protocol.union.dto.EthAddress
import com.rarible.protocol.union.dto.FlowAddress
import com.rarible.protocol.union.dto.FlowContract
import com.rarible.protocol.union.dto.serializer.eth.EthAddressDeserializer
import com.rarible.protocol.union.dto.serializer.eth.EthAddressSerializer
import com.rarible.protocol.union.dto.serializer.flow.FlowAddressDeserializer
import com.rarible.protocol.union.dto.serializer.flow.FlowAddressSerializer
import com.rarible.protocol.union.dto.serializer.flow.FlowContractDeserializer
import com.rarible.protocol.union.dto.serializer.flow.FlowContractSerializer

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