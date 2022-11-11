package com.rarible.protocol.union.core.converter

import com.rarible.protocol.union.core.model.UnionOwnership
import com.rarible.protocol.union.dto.ItemOwnershipDto
import org.slf4j.LoggerFactory

object ItemOwnershipConverter {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun convert(ownership: UnionOwnership): ItemOwnershipDto {
        try {
            return ItemOwnershipDto(
                id = ownership.id,
                blockchain = ownership.id.blockchain,
                collection = ownership.collection,
                owner = ownership.id.owner,
                creators = ownership.creators,
                value = ownership.value,
                lazyValue = ownership.lazyValue,
                createdAt = ownership.createdAt
            )
        } catch (e: Exception) {
            logger.error("Failed to convert Item's Ownership: {} \n{}", ownership.id.fullId(), e.message)
            throw e
        }
    }
}
