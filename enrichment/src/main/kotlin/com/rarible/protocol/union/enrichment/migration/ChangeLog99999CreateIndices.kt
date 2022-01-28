package com.rarible.protocol.union.enrichment.migration

import com.github.cloudyrock.mongock.ChangeLog
import com.github.cloudyrock.mongock.ChangeSet
import com.rarible.protocol.union.enrichment.repository.ItemReconciliationMarkRepository
import com.rarible.protocol.union.enrichment.repository.ItemRepository
import com.rarible.protocol.union.enrichment.repository.OwnershipReconciliationMarkRepository
import com.rarible.protocol.union.enrichment.repository.OwnershipRepository
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
        @NonLockGuarded ownershipReconciliationMarkRepository: OwnershipReconciliationMarkRepository,
        @NonLockGuarded itemReconciliationMarkRepository: ItemReconciliationMarkRepository
    ) = runBlocking {
        ownershipRepository.createIndices()
        itemRepository.createIndices()
        ownershipReconciliationMarkRepository.createIndices()
        itemReconciliationMarkRepository.createIndices()
    }
}