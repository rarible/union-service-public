package com.rarible.protocol.union.core.model.elastic

import com.rarible.protocol.union.core.model.elastic.EsEntitiesConfig.loadMapping
import com.rarible.protocol.union.core.model.elastic.EsEntitiesConfig.loadSettings
import com.rarible.protocol.union.dto.ActivityDto
import com.rarible.protocol.union.dto.ActivityTypeDto
import com.rarible.protocol.union.dto.BlockchainDto
import org.springframework.data.annotation.Id
import org.springframework.data.elasticsearch.annotations.Field
import org.springframework.data.elasticsearch.annotations.FieldType
import java.time.Instant

sealed class EsActivitySealed {
    abstract val activityId: String // blockchain:value
    abstract val blockchain: BlockchainDto
    abstract val type: ActivityTypeDto

    // TODO: replace with single cursor field?
    abstract val date: Instant
    abstract val blockNumber: Long?
    abstract val logIndex: Int?
    abstract val salt: Long
}

data class EsActivityLite(
    override val activityId: String, // blockchain:value
    override val blockchain: BlockchainDto,
    override val type: ActivityTypeDto,
    override val date: Instant,
    override val blockNumber: Long?,
    override val logIndex: Int?,
    override val salt: Long,
) : EsActivitySealed()

data class EsActivity(
    @Id
    override val activityId: String, // blockchain:value
    // Sort fields
    @Field(type = FieldType.Date)
    override val date: Instant,
    override val blockNumber: Long?,
    override val logIndex: Int?,
    override val salt: Long = generateSalt(),
    // Filter fields
    override val blockchain: BlockchainDto,
    override val type: ActivityTypeDto,
    val userFrom: String?,
    val userTo: String?,
    val collection: String?,
    val item: String,
    val sellCurrency: String? = null,
    val volumeUsd: Double? = null,
    val volumeSell: Double? = null,
    val volumeNative: Double? = null,
    val activityDto: String? = null,
) : EsActivitySealed() {
    companion object {
        const val VERSION: Int = 2

        val ENTITY_DEFINITION = EsEntity.ACTIVITY.let {
            EntityDefinition(
                entity = it,
                mapping = loadMapping(it),
                versionData = VERSION,
                settings = loadSettings(it),
            )
        }
    }
}
