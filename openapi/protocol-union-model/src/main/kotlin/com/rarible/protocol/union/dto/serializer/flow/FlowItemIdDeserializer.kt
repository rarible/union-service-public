package com.rarible.protocol.union.dto.serializer.flow

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.node.ObjectNode
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.FlowAddress
import com.rarible.protocol.union.dto.FlowItemIdDto
import com.rarible.protocol.union.dto.serializer.IdParser
import java.math.BigInteger

object FlowItemIdDeserializer : StdDeserializer<FlowItemIdDto>(FlowItemIdDto::class.java) {

    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): FlowItemIdDto? {
        val tree: ObjectNode = p.codec.readTree(p) ?: return null
        val value = tree.get(FlowItemIdDto::value.name)
        val token = tree.get(FlowItemIdDto::token.name)
        val tokenId = tree.get(FlowItemIdDto::tokenId.name)
        return FlowItemIdDto(
            value = IdParser.parse(value.textValue(), BlockchainDto.FLOW).second,
            token = token.traverse(p.codec).readValueAs(FlowAddress::class.java),
            tokenId = tokenId.traverse(p.codec).readValueAs(BigInteger::class.java)
        )
    }
}