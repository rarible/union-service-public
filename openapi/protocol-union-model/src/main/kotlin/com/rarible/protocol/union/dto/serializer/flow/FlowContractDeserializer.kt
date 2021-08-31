package com.rarible.protocol.union.dto.serializer.flow

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.FlowContract
import com.rarible.protocol.union.dto.serializer.IdParser

object FlowContractDeserializer : StdDeserializer<FlowContract>(FlowContract::class.java) {

    override fun deserialize(p: JsonParser, ctxt: DeserializationContext?): FlowContract? {
        val value = p.codec.readValue(p, String::class.java) ?: return null
        return FlowContract(IdParser.parse(value, BlockchainDto.FLOW).second)

    }
}