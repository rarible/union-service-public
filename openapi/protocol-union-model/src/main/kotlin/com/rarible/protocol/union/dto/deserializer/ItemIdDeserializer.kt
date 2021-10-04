package com.rarible.protocol.union.dto.deserializer

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.node.ObjectNode
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.UnionAddress
import java.math.BigInteger

object ItemIdDeserializer : StdDeserializer<ItemIdDto>(ItemIdDto::class.java) {

    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): ItemIdDto? {
        val tree: ObjectNode = p.codec.readTree(p) ?: return null
        val blockchain = tree.get(ItemIdDto::blockchain.name)
        val token = tree.get(ItemIdDto::token.name)
        val tokenId = tree.get(ItemIdDto::tokenId.name)
        return ItemIdDto(
            blockchain = BlockchainDto.valueOf(blockchain.textValue()),
            token = token.traverse(p.codec).readValueAs(UnionAddress::class.java),
            tokenId = tokenId.traverse(p.codec).readValueAs(BigInteger::class.java)
        )
    }
}