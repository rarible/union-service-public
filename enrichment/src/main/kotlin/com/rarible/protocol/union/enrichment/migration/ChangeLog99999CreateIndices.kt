package com.rarible.protocol.union.enrichment.migration

import com.github.cloudyrock.mongock.ChangeLog
import com.github.cloudyrock.mongock.ChangeSet
import com.rarible.protocol.union.enrichment.repository.ActivityRepository
import com.rarible.protocol.union.enrichment.repository.CollectionRepository
import com.rarible.protocol.union.enrichment.repository.ItemMetaRepository
import com.rarible.protocol.union.enrichment.repository.ItemRepository
import com.rarible.protocol.union.enrichment.repository.MetaAutoRefreshStateRepository
import com.rarible.protocol.union.enrichment.repository.MetaRefreshRequestRepository
import com.rarible.protocol.union.enrichment.repository.OwnershipRepository
import com.rarible.protocol.union.enrichment.repository.ReconciliationMarkRepository
import io.changock.migration.api.annotations.NonLockGuarded
import kotlinx.coroutines.runBlocking

@ChangeLog(order = "99999")
class ChangeLog99999CreateIndices {

    @ChangeSet(
        id = "ChangeLog99999CreateIndices.createIndicesForAllCollections",
        order = "99999",
        author = "protocol",
        runAlways = true
    )
    fun createIndicesForAllCollections(
        @NonLockGuarded ownershipRepository: OwnershipRepository,
        @NonLockGuarded itemRepository: ItemRepository,
        @NonLockGuarded itemReconciliationMarkRepository: ReconciliationMarkRepository,
        @NonLockGuarded collectionRepository: CollectionRepository,
        @NonLockGuarded itemMetaRepository: ItemMetaRepository,
        @NonLockGuarded activityRepository: ActivityRepository,
        @NonLockGuarded metaAutoRefreshStateRepository: MetaAutoRefreshStateRepository,
        @NonLockGuarded metaRefreshRequestRepository: MetaRefreshRequestRepository,
    ) = runBlocking {
        ownershipRepository.createIndices()
        itemRepository.createIndices()
        itemReconciliationMarkRepository.createIndices()
        collectionRepository.createIndices()
        activityRepository.createIndices()
        metaAutoRefreshStateRepository.createIndices()
        metaRefreshRequestRepository.createIndices()
    }
}
