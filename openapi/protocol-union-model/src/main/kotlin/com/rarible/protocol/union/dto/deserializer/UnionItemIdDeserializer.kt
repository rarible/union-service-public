package com.rarible.protocol.union.dto.deserializer

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.node.ObjectNode
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.UnionAddress
import com.rarible.protocol.union.dto.UnionItemIdDto
import java.math.BigInteger

object UnionItemIdDeserializer : StdDeserializer<UnionItemIdDto>(UnionItemIdDto::class.java) {

    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): UnionItemIdDto? {
        val tree: ObjectNode = p.codec.readTree(p) ?: return null
        val blockchain = tree.get(UnionItemIdDto::blockchain.name)
        val token = tree.get(UnionItemIdDto::token.name)
        val tokenId = tree.get(UnionItemIdDto::tokenId.name)
        return UnionItemIdDto(
            blockchain = BlockchainDto.valueOf(blockchain.textValue()),
            token = token.traverse(p.codec).readValueAs(UnionAddress::class.java),
            tokenId = tokenId.traverse(p.codec).readValueAs(BigInteger::class.java)
        )
    }
}