package com.rarible.protocol.union.integration.tezos.converter

import com.rarible.protocol.tezos.dto.NftOwnershipDto
import com.rarible.protocol.tezos.dto.NftOwnershipsDto
import com.rarible.protocol.union.core.converter.UnionAddressConverter
import com.rarible.protocol.union.core.model.UnionOwnership
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.OwnershipIdDto
import com.rarible.protocol.union.dto.continuation.page.Page
import org.slf4j.LoggerFactory

object TezosOwnershipConverter {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun convert(ownership: NftOwnershipDto, blockchain: BlockchainDto): UnionOwnership {
        try {
            return convertInternal(ownership, blockchain)
        } catch (e: Exception) {
            logger.error("Failed to convert {} Ownership: {} \n{}", blockchain, e.message, ownership)
            throw e
        }
    }

    private fun convertInternal(ownership: NftOwnershipDto, blockchain: BlockchainDto): UnionOwnership {
        val tokenId = ownership.tokenId
        val owner = UnionAddressConverter.convert(blockchain, ownership.owner)

        return UnionOwnership(
            id = OwnershipIdDto(
                blockchain = blockchain,
                contract = ownership.contract,
                tokenId = tokenId,
                owner = owner
            ),
            collection = CollectionIdDto(blockchain, ownership.contract),
            value = ownership.value,
            createdAt = ownership.createdAt,
            lastUpdatedAt = null, // TODO TEZOS add field
            creators = ownership.creators.map { TezosConverter.convertToCreator(it, blockchain) },
            lazyValue = ownership.lazyValue,
            pending = emptyList() // In Union we won't use this field for Tezos
        )
    }

    fun convert(page: NftOwnershipsDto, blockchain: BlockchainDto): Page<UnionOwnership> {
        return Page(
            total = page.total.toLong(),
            continuation = page.continuation,
            entities = page.ownerships.map { convert(it, blockchain) }
        )
    }
}

