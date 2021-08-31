package com.rarible.protocol.union.dto.serializer.eth

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.node.ObjectNode
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.EthAddress
import com.rarible.protocol.union.dto.EthOwnershipIdDto
import com.rarible.protocol.union.dto.serializer.IdParser
import java.math.BigInteger

object EthOwnershipIdDeserializer : StdDeserializer<EthOwnershipIdDto>(EthOwnershipIdDto::class.java) {

    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): EthOwnershipIdDto? {
        val tree: ObjectNode = p.codec.readTree(p) ?: return null
        val value = tree.get(EthOwnershipIdDto::value.name)
        val token = tree.get(EthOwnershipIdDto::token.name)
        val tokenId = tree.get(EthOwnershipIdDto::tokenId.name)
        val owner = tree.get(EthOwnershipIdDto::owner.name)
        return EthOwnershipIdDto(
            value = IdParser.parse(value.textValue(), BlockchainDto.ETHEREUM).second,
            token = token.traverse(p.codec).readValueAs(EthAddress::class.java),
            tokenId = tokenId.traverse(p.codec).readValueAs(BigInteger::class.java),
            owner = owner.traverse(p.codec).readValueAs(EthAddress::class.java)
        )
    }
}