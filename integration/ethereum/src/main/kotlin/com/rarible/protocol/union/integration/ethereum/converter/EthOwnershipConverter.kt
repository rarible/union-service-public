package com.rarible.protocol.union.integration.ethereum.converter

import com.rarible.protocol.dto.ItemRoyaltyDto
import com.rarible.protocol.dto.ItemTransferDto
import com.rarible.protocol.dto.NftOwnershipDeleteEventDto
import com.rarible.protocol.dto.NftOwnershipDto
import com.rarible.protocol.dto.NftOwnershipEventDto
import com.rarible.protocol.dto.NftOwnershipUpdateEventDto
import com.rarible.protocol.dto.NftOwnershipsDto
import com.rarible.protocol.union.core.converter.UnionAddressConverter
import com.rarible.protocol.union.core.model.UnionOwnership
import com.rarible.protocol.union.core.model.UnionOwnershipDeleteEvent
import com.rarible.protocol.union.core.model.UnionOwnershipEvent
import com.rarible.protocol.union.core.model.UnionOwnershipUpdateEvent
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.ItemHistoryDto
import com.rarible.protocol.union.dto.OwnershipIdDto
import com.rarible.protocol.union.dto.continuation.page.Page
import org.slf4j.LoggerFactory

object EthOwnershipConverter {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun convert(source: NftOwnershipDto, blockchain: BlockchainDto): UnionOwnership {
        try {
            return convertInternal(source, blockchain)
        } catch (e: Exception) {
            logger.error("Failed to convert {} Ownership: {} \n{}", blockchain, e.message, source)
            throw e
        }
    }

    suspend fun convert(event: NftOwnershipEventDto, blockchain: BlockchainDto): UnionOwnershipEvent {
        logger.info("Received {} Ownership event: {}", blockchain, event)
        val eventTimeMarks = EthConverter.convert(event.eventTimeMarks)

        return when (event) {
            is NftOwnershipUpdateEventDto -> {
                val ownership = convert(event.ownership, blockchain)
                UnionOwnershipUpdateEvent(ownership, eventTimeMarks)
            }

            is NftOwnershipDeleteEventDto -> {
                val deletedOwnership = event.ownership!!
                val ownershipId = OwnershipIdDto(
                    blockchain = blockchain,
                    contract = EthConverter.convert(deletedOwnership.token),
                    tokenId = deletedOwnership.tokenId,
                    owner = UnionAddressConverter.convert(blockchain, EthConverter.convert(deletedOwnership.owner))
                )
                UnionOwnershipDeleteEvent(ownershipId, eventTimeMarks)
            }
        }
    }

    private fun convertInternal(source: NftOwnershipDto, blockchain: BlockchainDto): UnionOwnership {
        val contract = EthConverter.convert(source.contract)
        return UnionOwnership(
            id = OwnershipIdDto(
                contract = contract,
                tokenId = source.tokenId,
                owner = EthConverter.convert(source.owner, blockchain),
                blockchain = blockchain
            ),
            collection = CollectionIdDto(blockchain, contract),
            value = source.value,
            createdAt = source.date,
            lastUpdatedAt = source.lastUpdatedAt,
            creators = (source.creators ?: emptyList()).map { EthConverter.convertToCreator(it, blockchain) },
            lazyValue = source.lazyValue,
            pending = source.pending.map { convert(it, blockchain) }
        )
    }

    fun convert(page: NftOwnershipsDto, blockchain: BlockchainDto): Page<UnionOwnership> {
        return Page(
            total = page.total ?: 0,
            continuation = page.continuation,
            entities = page.ownerships.map { convert(it, blockchain) }
        )
    }

    private fun convert(source: com.rarible.protocol.dto.ItemHistoryDto, blockchain: BlockchainDto): ItemHistoryDto {
        return when (source) {
            is ItemRoyaltyDto -> EthItemConverter.convert(source, blockchain)
            is ItemTransferDto -> EthItemConverter.convert(source, blockchain)
        }
    }
}
