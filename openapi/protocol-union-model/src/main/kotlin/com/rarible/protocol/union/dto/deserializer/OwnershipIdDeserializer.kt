package com.rarible.protocol.union.dto.deserializer

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.node.ObjectNode
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.OwnershipIdDto
import com.rarible.protocol.union.dto.UnionAddress
import java.math.BigInteger

object OwnershipIdDeserializer : StdDeserializer<OwnershipIdDto>(OwnershipIdDto::class.java) {

    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): OwnershipIdDto? {
        val tree: ObjectNode = p.codec.readTree(p) ?: return null
        val blockchain = tree.get(OwnershipIdDto::blockchain.name)
        val token = tree.get(OwnershipIdDto::token.name)
        val tokenId = tree.get(OwnershipIdDto::tokenId.name)
        val owner = tree.get(OwnershipIdDto::owner.name)
        return OwnershipIdDto(
            blockchain = BlockchainDto.valueOf(blockchain.textValue()),
            token = token.traverse(p.codec).readValueAs(UnionAddress::class.java),
            tokenId = tokenId.traverse(p.codec).readValueAs(BigInteger::class.java),
            owner = owner.traverse(p.codec).readValueAs(UnionAddress::class.java)
        )
    }
}