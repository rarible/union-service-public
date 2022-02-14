package com.rarible.protocol.union.listener.job.task

import com.rarible.core.task.RunTask
import com.rarible.core.task.TaskHandler
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.parser.IdParser
import com.rarible.protocol.union.enrichment.model.ShortItemId
import com.rarible.protocol.union.enrichment.repository.DefaultItemRepository
import com.rarible.protocol.union.enrichment.repository.legacy.LegacyItemRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.springframework.stereotype.Component

@Component
class ItemCollectionMigrateTask(
    private val legacyItemRepository: LegacyItemRepository,
    private val defaultItemRepository: DefaultItemRepository
) : TaskHandler<String> {

    override val type = "ITEM_MIGRATION_TASK"

    override fun getAutorunParams(): List<RunTask> {
        return listOf(RunTask(""))
    }

    override fun runLongTask(from: String?, param: String): Flow<String> {
        val itemId = from?.let { IdParser.parseItemId(it) }?.let { ShortItemId(it) }
        val legacy = legacyItemRepository.findByBlockchain(
            itemId,
            null,
            Int.MAX_VALUE
        )

        return legacy.map {
            if (it.id.blockchain != BlockchainDto.SOLANA) {
                val currentVersion = defaultItemRepository.get(it.id)?.version
                val updated = it.copy(version = currentVersion)
                defaultItemRepository.save(updated)
            }
            it.id.toDto().fullId()
        }
    }
}