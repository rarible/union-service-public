package com.rarible.protocol.union.core.task

import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.SyncTypeDto

sealed class TaskParam {
    abstract val versionData: Int?
    abstract val settingsHash: String?
    abstract val index: String
    abstract val blockchain: BlockchainDto
    abstract val from: Long?
    abstract val to: Long?
    abstract val tags: List<String>?
}

data class RawTaskParam(
    override val versionData: Int?,
    override val settingsHash: String?,
    override val blockchain: BlockchainDto,
    override val index: String,
    override val from: Long? = null,
    override val to: Long? = null,
    override val tags: List<String>? = null,
) : TaskParam()

/**
 * Reindexing task state
 *
 * @param blockchain blockchain
 * @param type activity type
 * @param index elasticsearch index name, including environment, version, etc.
 */
data class ActivityTaskParam(
    override val versionData: Int?,
    override val settingsHash: String?,
    override val blockchain: BlockchainDto,
    val type: SyncTypeDto,
    override val index: String,
    override val from: Long? = null,
    override val to: Long? = null,
    override val tags: List<String>? = null,
) : TaskParam()

data class RemoveRevertedActivityTaskParam(
    val blockchain: BlockchainDto,
    val type: SyncTypeDto
)

data class OrderTaskParam(
    override val versionData: Int?,
    override val settingsHash: String?,
    override val blockchain: BlockchainDto,
    override val index: String,
    override val from: Long? = null,
    override val to: Long? = null,
    override val tags: List<String>? = null,
) : TaskParam()

data class ItemTaskParam(
    override val versionData: Int?,
    override val settingsHash: String?,
    override val blockchain: BlockchainDto,
    override val index: String,
    override val from: Long? = null,
    override val to: Long? = null,
    override val tags: List<String>? = null,
) : TaskParam()

data class CollectionTaskParam(
    override val versionData: Int?,
    override val settingsHash: String?,
    override val blockchain: BlockchainDto,
    override val index: String,
    override val from: Long? = null,
    override val to: Long? = null,
    override val tags: List<String>? = null,
) : TaskParam()

/**
 * Reindexing task state
 *
 * @param blockchain blockchain
 * @param index elasticsearch index name, including environment, version, etc.
 */
data class OwnershipTaskParam(
    override val versionData: Int?,
    override val settingsHash: String?,
    override val blockchain: BlockchainDto,
    val target: Target,
    override val index: String,
    override val from: Long? = null,
    override val to: Long? = null,
    override val tags: List<String>? = null,
) : TaskParam() {
    enum class Target {
        OWNERSHIP,
        AUCTIONED_OWNERSHIP,
    }
}

data class SyncTraitJobParam(
    override val blockchain: BlockchainDto,
    override val scope: SyncScope,
    val collectionId: String? = null,
    override val esIndex: String? = null,
    override val batchSize: Int = DEFAULT_BATCH,
    override val chunkSize: Int = DEFAULT_CHUNK,
) : AbstractSyncJobParam() {

    companion object {
        val TYPE = "SYNC_TRAIT_TASK"
    }
}
