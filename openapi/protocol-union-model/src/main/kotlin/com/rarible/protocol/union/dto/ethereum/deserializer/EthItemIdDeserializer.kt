package com.rarible.protocol.union.dto.ethereum.deserializer

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.node.ObjectNode
import com.rarible.protocol.union.dto.EthBlockchainDto
import com.rarible.protocol.union.dto.EthItemIdDto
import com.rarible.protocol.union.dto.IdParser
import com.rarible.protocol.union.dto.ethereum.EthAddress
import java.math.BigInteger

object EthItemIdDeserializer : StdDeserializer<EthItemIdDto>(EthItemIdDto::class.java) {

    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): EthItemIdDto? {
        val tree: ObjectNode = p.codec.readTree(p) ?: return null
        val value = tree.get(EthItemIdDto::value.name)
        val blockchain = tree.get(EthItemIdDto::blockchain.name)
        val token = tree.get(EthItemIdDto::token.name)
        val tokenId = tree.get(EthItemIdDto::tokenId.name)
        return EthItemIdDto(
            value = IdParser.parse(value.textValue()).second,
            blockchain = EthBlockchainDto.valueOf(blockchain.textValue()),
            token = token.traverse(p.codec).readValueAs(EthAddress::class.java),
            tokenId = tokenId.traverse(p.codec).readValueAs(BigInteger::class.java)
        )
    }
}