package com.rarible.protocol.union.api.service

import com.rarible.protocol.union.core.converter.UnionConverter
import com.rarible.protocol.union.core.exception.UnionValidationException
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.parser.IdParser
import com.rarible.protocol.union.dto.parser.ItemIdParser

// TODO UNION - Remove later
// We need this method in order to support compatibility during moving from contract/tokenId params to
// single itemId param
fun extractItemId(contract: String?, tokenId: String?, itemId: String?): ItemIdDto {
    if (itemId != null) {
        return ItemIdParser.parseFull(itemId)
    } else if (contract != null && tokenId != null) {
        val address = IdParser.parseAddress(contract)
        return ItemIdDto(
            // Back compatibility with existing API, works until Polygon enabled
            blockchain = IdParser.parseBlockchain(address.blockchainGroup.name),
            contract = address.value,
            tokenId = UnionConverter.convertToBigInteger(tokenId)
        )
    } else {
        throw UnionValidationException("ItemId not specified ('contract' + 'tokenId' params OR itemId param")
    }
}