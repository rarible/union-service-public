package com.rarible.protocol.union.dto.flow.deserializer

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.node.ObjectNode
import com.rarible.protocol.union.dto.FlowBlockchainDto
import com.rarible.protocol.union.dto.FlowOwnershipIdDto
import com.rarible.protocol.union.dto.IdParser
import com.rarible.protocol.union.dto.flow.FlowAddress
import com.rarible.protocol.union.dto.flow.FlowContract
import java.math.BigInteger

object FlowOwnershipIdDeserializer : StdDeserializer<FlowOwnershipIdDto>(FlowOwnershipIdDto::class.java) {

    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): FlowOwnershipIdDto? {
        val tree: ObjectNode = p.codec.readTree(p) ?: return null
        val value = tree.get(FlowOwnershipIdDto::value.name)
        val blockchain = tree.get(FlowOwnershipIdDto::blockchain.name)
        val token = tree.get(FlowOwnershipIdDto::token.name)
        val tokenId = tree.get(FlowOwnershipIdDto::tokenId.name)
        val owner = tree.get(FlowOwnershipIdDto::owner.name)
        return FlowOwnershipIdDto(
            value = IdParser.parse(value.textValue()).second,
            blockchain = FlowBlockchainDto.valueOf(blockchain.textValue()),
            token = token.traverse(p.codec).readValueAs(FlowContract::class.java),
            tokenId = tokenId.traverse(p.codec).readValueAs(BigInteger::class.java),
            owner = owner.traverse(p.codec).readValueAs(FlowAddress::class.java)
        )
    }
}