package com.rarible.protocol.union.listener.job.task

import com.rarible.core.task.RunTask
import com.rarible.core.task.TaskHandler
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.parser.OwnershipIdParser
import com.rarible.protocol.union.enrichment.model.ShortOwnershipId
import com.rarible.protocol.union.enrichment.repository.DefaultOwnershipRepository
import com.rarible.protocol.union.enrichment.repository.legacy.LegacyOwnershipRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.springframework.stereotype.Component

@Component
class OwnershipCollectionMigrateTask(
    private val legacyOwnershipRepository: LegacyOwnershipRepository,
    private val defaultOwnershipRepository: DefaultOwnershipRepository
) : TaskHandler<String> {

    override val type = "OWNERSHIP_MIGRATION_TASK"

    override fun getAutorunParams(): List<RunTask> {
        return listOf(RunTask(""))
    }

    override fun runLongTask(from: String?, param: String): Flow<String> {
        val itemId = from?.let { OwnershipIdParser.parseFull(it) }?.let { ShortOwnershipId(it) }
        val legacy = legacyOwnershipRepository.findAll(itemId)

        return legacy.map {
            if (it.id.blockchain != BlockchainDto.SOLANA) {
                val currentVersion = defaultOwnershipRepository.get(it.id)?.version
                val updated = it.copy(version = currentVersion)
                defaultOwnershipRepository.save(updated)
            }
            it.id.toDto().fullId()
        }
    }
}