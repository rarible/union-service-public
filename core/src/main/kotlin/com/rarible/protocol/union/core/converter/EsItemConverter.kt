package com.rarible.protocol.union.core.converter

import com.rarible.protocol.union.core.model.EsItem
import com.rarible.protocol.union.core.model.EsTrait
import com.rarible.protocol.union.dto.ItemDto
import com.rarible.protocol.union.dto.parser.IdParser

object EsItemConverter {

    fun ItemDto.toEsItem(): EsItem {
        return EsItem(
            itemId = id.fullId(),
            blockchain = blockchain,
            collection = collection?.fullId(),
            name = meta?.name,
            description = meta?.description,
            creators = creators.map { it.account.fullId() },
            mintedAt = mintedAt,
            lastUpdatedAt = lastUpdatedAt,
            owner = null,
            traits = meta?.attributes?.map { EsTrait(it.key, it.value) } ?: emptyList()
        )
    }
}
