package com.rarible.protocol.union.dto.deserializer

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.node.ObjectNode
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.UnionAddress
import com.rarible.protocol.union.dto.UnionOwnershipIdDto
import java.math.BigInteger

object UnionOwnershipIdDeserializer : StdDeserializer<UnionOwnershipIdDto>(UnionOwnershipIdDto::class.java) {

    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): UnionOwnershipIdDto? {
        val tree: ObjectNode = p.codec.readTree(p) ?: return null
        val blockchain = tree.get(UnionOwnershipIdDto::blockchain.name)
        val token = tree.get(UnionOwnershipIdDto::token.name)
        val tokenId = tree.get(UnionOwnershipIdDto::tokenId.name)
        val owner = tree.get(UnionOwnershipIdDto::owner.name)
        return UnionOwnershipIdDto(
            blockchain = BlockchainDto.valueOf(blockchain.textValue()),
            token = token.traverse(p.codec).readValueAs(UnionAddress::class.java),
            tokenId = tokenId.traverse(p.codec).readValueAs(BigInteger::class.java),
            owner = owner.traverse(p.codec).readValueAs(UnionAddress::class.java)
        )
    }
}