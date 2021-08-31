package com.rarible.protocol.union.dto.serializer.eth

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.node.ObjectNode
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.EthAddress
import com.rarible.protocol.union.dto.EthItemIdDto
import com.rarible.protocol.union.dto.serializer.IdParser
import java.math.BigInteger

object EthItemIdDeserializer : StdDeserializer<EthItemIdDto>(EthItemIdDto::class.java) {

    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): EthItemIdDto? {
        val tree: ObjectNode = p.codec.readTree(p) ?: return null
        val value = tree.get(EthItemIdDto::value.name)
        val token = tree.get(EthItemIdDto::token.name)
        val tokenId = tree.get(EthItemIdDto::tokenId.name)
        return EthItemIdDto(
            value = IdParser.parse(value.textValue(), BlockchainDto.ETHEREUM).second,
            token = token.traverse(p.codec).readValueAs(EthAddress::class.java),
            tokenId = tokenId.traverse(p.codec).readValueAs(BigInteger::class.java)
        )
    }
}