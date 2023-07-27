package com.rarible.protocol.union.integration.tezos.dipdup.converter

import com.rarible.dipdup.client.core.model.DipDupOwnership
import com.rarible.protocol.union.core.converter.UnionAddressConverter
import com.rarible.protocol.union.core.model.UnionOwnership
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.OwnershipIdDto
import com.rarible.protocol.union.dto.continuation.page.Page
import org.slf4j.LoggerFactory
import java.math.BigInteger

object DipDupOwnershipConverter {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val blockchain = BlockchainDto.TEZOS

    fun convert(ownership: DipDupOwnership): UnionOwnership {
        try {
            return convertInternal(ownership, blockchain)
        } catch (e: Exception) {
            logger.error("Failed to convert {} Ownership: {} \n{}", blockchain, e.message, ownership)
            throw e
        }
    }

    fun convert(dipdupPage: com.rarible.dipdup.client.model.Page<DipDupOwnership>, blockchain: BlockchainDto): Page<UnionOwnership> {
        return Page(
            total = dipdupPage.items.size.toLong(),
            continuation = dipdupPage.continuation,
            entities = dipdupPage.items.map { convertInternal(it, blockchain) }
        )
    }

    private fun convertInternal(ownership: DipDupOwnership, blockchain: BlockchainDto): UnionOwnership {
        val tokenId = ownership.tokenId
        val owner = UnionAddressConverter.convert(blockchain, ownership.owner)
        val contract = ownership.contract

        return UnionOwnership(
            id = OwnershipIdDto(
                blockchain = blockchain,
                contract = contract,
                tokenId = tokenId,
                owner = owner
            ),
            collection = CollectionIdDto(blockchain, contract),
            value = ownership.balance,
            createdAt = ownership.created,
            lastUpdatedAt = ownership.updated,
            creators = emptyList(), // Deprecated field
            lazyValue = BigInteger.ZERO, // lazy isn't supported yet
            pending = emptyList() // In Union we won't use this field for Tezos
        )
    }
}
