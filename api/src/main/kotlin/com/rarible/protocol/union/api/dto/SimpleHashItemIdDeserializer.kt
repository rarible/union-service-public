package com.rarible.protocol.union.api.dto

import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.parser.IdParser

object SimpleHashItemIdDeserializer {
    fun parse(nftId: String): ItemIdDto {
        // SimpleHash itemId format is "ethereum.0x8943c7bac1914c9a7aba750bf2b6b09fd21037e0.5903"
        val toUnionFormat = nftId.replace(".",":").uppercase()
        return IdParser.parseItemId(toUnionFormat)
    }
}