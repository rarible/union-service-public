package com.rarible.protocol.union.dto.ethereum.deserializer

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.node.ObjectNode
import com.rarible.protocol.union.dto.EthBlockchainDto
import com.rarible.protocol.union.dto.ethereum.EthAddress
import com.rarible.protocol.union.dto.ethereum.EthOwnershipIdDto
import java.math.BigInteger

object EthOwnershipIdDeserializer : StdDeserializer<EthOwnershipIdDto>(EthOwnershipIdDto::class.java) {

    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): EthOwnershipIdDto? {
        val tree: ObjectNode = p.codec.readTree(p) ?: return null
        val blockchain = tree.get(EthOwnershipIdDto::blockchain.name)
        val token = tree.get(EthOwnershipIdDto::token.name)
        val tokenId = tree.get(EthOwnershipIdDto::tokenId.name)
        val owner = tree.get(EthOwnershipIdDto::owner.name)
        return EthOwnershipIdDto(
            blockchain = EthBlockchainDto.valueOf(blockchain.textValue()),
            token = token.traverse(p.codec).readValueAs(EthAddress::class.java),
            tokenId = tokenId.traverse(p.codec).readValueAs(BigInteger::class.java),
            owner = owner.traverse(p.codec).readValueAs(EthAddress::class.java)
        )
    }
}