package com.rarible.protocol.union.integration.flow.converter

import com.rarible.protocol.dto.FlowNftOwnershipDto
import com.rarible.protocol.dto.FlowNftOwnershipsDto
import com.rarible.protocol.union.core.converter.UnionAddressConverter
import com.rarible.protocol.union.core.model.UnionOwnership
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.OwnershipIdDto
import com.rarible.protocol.union.dto.continuation.page.Page
import org.slf4j.LoggerFactory
import java.math.BigInteger

object FlowOwnershipConverter {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun convert(ownership: FlowNftOwnershipDto, blockchain: BlockchainDto): UnionOwnership {
        try {
            return convertInternal(ownership, blockchain)
        } catch (e: Exception) {
            logger.error("Failed to convert {} Ownership: {} \n{}", blockchain, e.message, ownership)
            throw e
        }
    }

    private fun convertInternal(ownership: FlowNftOwnershipDto, blockchain: BlockchainDto): UnionOwnership {
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
            value = BigInteger.ONE, // TODO FLOW always one?
            createdAt = ownership.createdAt,
            lastUpdatedAt = null, // TODO FLOW add field
            lazyValue = BigInteger.ZERO,
            creators = ownership.creators.map { FlowConverter.convertToCreator(it, blockchain) }
        )
    }

    fun convert(page: FlowNftOwnershipsDto, blockchain: BlockchainDto): Page<UnionOwnership> {
        return Page(
            total = page.total,
            continuation = page.continuation,
            entities = page.ownerships.map { convert(it, blockchain) }
        )
    }
}

