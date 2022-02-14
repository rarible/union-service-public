package com.rarible.protocol.union.enrichment.repository

import com.mongodb.client.result.DeleteResult
import com.rarible.protocol.union.enrichment.model.ItemSellStats
import com.rarible.protocol.union.enrichment.model.ShortItemId
import com.rarible.protocol.union.enrichment.model.ShortOwnership
import com.rarible.protocol.union.enrichment.model.ShortOwnershipId
import kotlinx.coroutines.flow.Flow
import java.time.Instant

interface OwnershipRepository {

    suspend fun createIndices()

    suspend fun save(ownership: ShortOwnership): ShortOwnership

    suspend fun get(id: ShortOwnershipId): ShortOwnership?

    suspend fun getAll(ids: List<ShortOwnershipId>): List<ShortOwnership>

    fun findWithMultiCurrency(lastUpdateAt: Instant): Flow<ShortOwnership>

    suspend fun getItemSellStats(itemId: ShortItemId): ItemSellStats

    suspend fun delete(ownershipId: ShortOwnershipId): DeleteResult?

}