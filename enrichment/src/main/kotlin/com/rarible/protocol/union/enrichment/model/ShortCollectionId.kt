package com.rarible.protocol.union.enrichment.model

import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.parser.IdParser

data class ShortCollectionId(
    val blockchain: BlockchainDto,
    val collectionId: String
) {
    constructor(dto: CollectionIdDto) : this(
        dto.blockchain,
        dto.value
    )

    override fun toString(): String {
        return toDto().fullId()
    }

    fun toDto(): CollectionIdDto {
        return CollectionIdDto(
            blockchain = blockchain,
            value = collectionId
        )
    }

    companion object {

        fun of(collectionId: String): ShortCollectionId {
            return ShortCollectionId(IdParser.parseCollectionId(collectionId))
        }
    }

}