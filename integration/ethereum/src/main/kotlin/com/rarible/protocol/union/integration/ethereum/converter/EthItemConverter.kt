package com.rarible.protocol.union.integration.ethereum.converter

import com.rarible.core.common.nowMillis
import com.rarible.protocol.dto.LazyErc1155Dto
import com.rarible.protocol.dto.LazyErc721Dto
import com.rarible.protocol.dto.LazyNftDto
import com.rarible.protocol.dto.NftItemDeleteEventDto
import com.rarible.protocol.dto.NftItemDto
import com.rarible.protocol.dto.NftItemEventDto
import com.rarible.protocol.dto.NftItemUpdateEventDto
import com.rarible.protocol.dto.NftItemsDto
import com.rarible.protocol.union.core.converter.ContractAddressConverter
import com.rarible.protocol.union.core.exception.UnionException
import com.rarible.protocol.union.core.model.UnionEthLazyItemErc1155
import com.rarible.protocol.union.core.model.UnionEthLazyItemErc721
import com.rarible.protocol.union.core.model.UnionItem
import com.rarible.protocol.union.core.model.UnionItemDeleteEvent
import com.rarible.protocol.union.core.model.UnionItemEvent
import com.rarible.protocol.union.core.model.UnionItemUpdateEvent
import com.rarible.protocol.union.core.model.UnionLazyItem
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.ItemRoyaltyDto
import com.rarible.protocol.union.dto.ItemTransferDto
import com.rarible.protocol.union.dto.continuation.page.Page
import org.slf4j.LoggerFactory

object EthItemConverter {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun convert(item: NftItemDto, blockchain: BlockchainDto): UnionItem {
        try {
            return convertInternal(item, blockchain)
        } catch (e: Exception) {
            logger.error("Failed to convert {} Item: {} \n{}", blockchain, e.message, item)
            throw e
        }
    }

    fun convert(event: NftItemEventDto, blockchain: BlockchainDto): UnionItemEvent {
        val eventTimeMarks = EthConverter.convert(event.eventTimeMarks)
        return when (event) {
            is NftItemUpdateEventDto -> {
                UnionItemUpdateEvent(
                    item = convert(event.item, blockchain),
                    eventTimeMarks = eventTimeMarks
                )
            }

            is NftItemDeleteEventDto -> {
                val itemId = ItemIdDto(
                    blockchain = blockchain,
                    contract = EthConverter.convert(event.item.token),
                    tokenId = event.item.tokenId
                )
                UnionItemDeleteEvent(
                    itemId = itemId,
                    eventTimeMarks = eventTimeMarks
                )
            }
        }
    }

    fun convert(source: LazyNftDto, blockchain: BlockchainDto): UnionLazyItem {
        return when (source) {
            is LazyErc721Dto -> UnionEthLazyItemErc721(
                id = ItemIdDto(blockchain, source.contract.prefixed(), source.tokenId),
                uri = source.uri,
                creators = source.creators.map { EthConverter.convertToCreator(it, blockchain) },
                royalties = source.royalties.map { EthConverter.convertToRoyalty(it, blockchain) },
                signatures = source.signatures.map { EthConverter.convert(it) }
            )

            is LazyErc1155Dto -> UnionEthLazyItemErc1155(
                id = ItemIdDto(blockchain, source.contract.prefixed(), source.tokenId),
                uri = source.uri,
                creators = source.creators.map { EthConverter.convertToCreator(it, blockchain) },
                royalties = source.royalties.map { EthConverter.convertToRoyalty(it, blockchain) },
                signatures = source.signatures.map { EthConverter.convert(it) },
                supply = source.supply
            )

            else -> throw UnionException("Unsupported Lazy Item type for $blockchain: ${source.javaClass.simpleName}")
        }
    }

    private fun convertInternal(item: NftItemDto, blockchain: BlockchainDto): UnionItem {
        val contract = EthConverter.convert(item.contract)
        return UnionItem(
            id = ItemIdDto(
                contract = contract,
                tokenId = item.tokenId,
                blockchain = blockchain
            ),
            collection = CollectionIdDto(blockchain, contract), // For ETH collection is a contract value
            mintedAt = item.mintedAt ?: item.lastUpdatedAt ?: nowMillis(), // TODO entire reduce required on Eth
            lastUpdatedAt = item.lastUpdatedAt ?: nowMillis(),
            supply = item.supply,
            meta = null, // Eth won't send us meta anymore, should be fetched via API
            deleted = item.deleted ?: false,
            creators = item.creators.map { EthConverter.convertToCreator(it, blockchain) },
            lazySupply = item.lazySupply,
            pending = item.pending?.map { convert(it, blockchain) } ?: listOf(),
            suspicious = item.isSuspiciousOnOS
        )
    }

    fun convert(page: NftItemsDto, blockchain: BlockchainDto): Page<UnionItem> {
        return Page(
            total = page.total ?: 0,
            continuation = page.continuation,
            entities = page.items.map { convert(it, blockchain) }
        )
    }

    fun convert(source: com.rarible.protocol.dto.ItemTransferDto, blockchain: BlockchainDto): ItemTransferDto {
        return ItemTransferDto(
            owner = EthConverter.convert(source.owner, blockchain),
            contract = ContractAddressConverter.convert(blockchain, EthConverter.convert(source.contract)),
            tokenId = source.tokenId,
            value = source.value,
            date = source.date,
            from = EthConverter.convert(source.from, blockchain)
        )
    }

    fun convert(source: com.rarible.protocol.dto.ItemRoyaltyDto, blockchain: BlockchainDto): ItemRoyaltyDto {
        return ItemRoyaltyDto(
            owner = source.owner?.let { EthConverter.convert(it, blockchain) },
            contract = ContractAddressConverter.convert(blockchain, EthConverter.convert(source.contract)),
            tokenId = source.tokenId,
            value = source.value!!,
            date = source.date,
            royalties = source.royalties.map { EthConverter.convertToRoyalty(it, blockchain) }
        )
    }
}
