package com.rarible.protocol.union.enrichment.repository

import com.mongodb.client.result.DeleteResult
import com.rarible.core.common.optimisticLock
import com.rarible.protocol.union.dto.AuctionIdDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.PlatformDto
import com.rarible.protocol.union.enrichment.model.ShortItem
import com.rarible.protocol.union.enrichment.model.ShortItemId
import com.rarible.protocol.union.enrichment.repository.legacy.LegacyItemRepository
import kotlinx.coroutines.flow.Flow
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Component
import java.time.Instant

@Component
@Primary
@Deprecated("Remove after migration")
class ItemRoutedRepository(
    private val legacyRepository: LegacyItemRepository,
    private val defaultRepository: DefaultItemRepository,
    @Value("\${common.useLegacyRepository:true}")
    private val useLegacy: Boolean
) : ItemRepository {

    private val repository = if (useLegacy) legacyRepository else defaultRepository

    override suspend fun createIndices() {
        legacyRepository.createIndices()
        defaultRepository.createIndices()
    }

    override suspend fun save(item: ShortItem): ShortItem {
        // For SOLANA we work only with new repository
        if (item.blockchain == BlockchainDto.SOLANA) {
            return defaultRepository.save(item)
        }

        if (useLegacy) {
            val result = legacyRepository.save(item)
            // In order to have correct version in target repo we need to execute additional GET here
            optimisticLock {
                val exist = defaultRepository.get(item.id)
                defaultRepository.save(item.copy(version = exist?.version))
            }
            return result
        }

        return repository.save(item)
    }

    override suspend fun get(id: ShortItemId): ShortItem? {
        return repository.get(id)
    }

    override suspend fun getAll(ids: List<ShortItemId>): List<ShortItem> {
        return repository.getAll(ids)
    }

    override fun findWithMultiCurrency(lastUpdateAt: Instant): Flow<ShortItem> {
        return repository.findWithMultiCurrency(lastUpdateAt)
    }

    override fun findByBlockchain(
        fromShortItemId: ShortItemId?, blockchain: BlockchainDto?, limit: Int
    ): Flow<ShortItem> {
        return repository.findByBlockchain(fromShortItemId, blockchain, limit)
    }

    override fun findByAuction(auctionId: AuctionIdDto): Flow<ShortItem> {
        return repository.findByAuction(auctionId)
    }

    override fun findByPlatformWithSell(
        platform: PlatformDto, fromShortItemId: ShortItemId?, limit: Int?
    ): Flow<ShortItem> {
        return repository.findByPlatformWithSell(platform, fromShortItemId, limit)
    }

    override suspend fun delete(itemId: ShortItemId): DeleteResult? {
        // For SOLANA we work only with new repository
        if (itemId.blockchain == BlockchainDto.SOLANA) {
            return defaultRepository.delete(itemId)
        }
        if (useLegacy) {
            val result = legacyRepository.delete(itemId)
            defaultRepository.delete(itemId)
            return result
        }
        return repository.delete(itemId)
    }

}
