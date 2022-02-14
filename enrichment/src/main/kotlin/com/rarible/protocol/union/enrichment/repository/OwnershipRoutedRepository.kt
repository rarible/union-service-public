package com.rarible.protocol.union.enrichment.repository

import com.mongodb.client.result.DeleteResult
import com.rarible.core.common.optimisticLock
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.enrichment.model.ItemSellStats
import com.rarible.protocol.union.enrichment.model.ShortItemId
import com.rarible.protocol.union.enrichment.model.ShortOwnership
import com.rarible.protocol.union.enrichment.model.ShortOwnershipId
import com.rarible.protocol.union.enrichment.repository.legacy.LegacyOwnershipRepository
import kotlinx.coroutines.flow.Flow
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Component
import java.time.Instant

@Primary
@Component
@Deprecated("Remove after migration")
class OwnershipRoutedRepository(
    private val legacyRepository: LegacyOwnershipRepository,
    private val defaultRepository: DefaultOwnershipRepository,
    @Value("\${common.useLegacyRepository:true}")
    private val useLegacy: Boolean
) : OwnershipRepository {

    private val repository = if (useLegacy) legacyRepository else defaultRepository

    override suspend fun createIndices() {
        defaultRepository.createIndices()
        legacyRepository.createIndices()
    }

    override suspend fun save(ownership: ShortOwnership): ShortOwnership {
        // For SOLANA we work only with new repository
        if (ownership.blockchain == BlockchainDto.SOLANA) {
            return defaultRepository.save(ownership)
        }

        if (useLegacy) {
            val result = legacyRepository.save(ownership)
            // In order to have correct version in target repo we need to execute additional GET here
            optimisticLock {
                val exist = defaultRepository.get(ownership.id)
                defaultRepository.save(ownership.copy(version = exist?.version))
            }
            return result
        }

        return repository.save(ownership)
    }

    override suspend fun get(id: ShortOwnershipId): ShortOwnership? {
        return repository.get(id)
    }

    override suspend fun getAll(ids: List<ShortOwnershipId>): List<ShortOwnership> {
        return repository.getAll(ids)
    }

    override fun findWithMultiCurrency(lastUpdateAt: Instant): Flow<ShortOwnership> {
        return repository.findWithMultiCurrency(lastUpdateAt)
    }

    override suspend fun getItemSellStats(itemId: ShortItemId): ItemSellStats {
        return repository.getItemSellStats(itemId)
    }

    override suspend fun delete(ownershipId: ShortOwnershipId): DeleteResult? {
        // For SOLANA we work only with new repository
        if (ownershipId.blockchain == BlockchainDto.SOLANA) {
            return defaultRepository.delete(ownershipId)
        }
        if (useLegacy) {
            val result = legacyRepository.delete(ownershipId)
            defaultRepository.delete(ownershipId)
            return result
        }
        return repository.delete(ownershipId)
    }

}
