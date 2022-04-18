package com.rarible.protocol.union.api.service.elastic

import com.rarible.protocol.union.api.service.UserActivityTypeConverter
import com.rarible.protocol.union.core.FeatureFlagsProperties
import com.rarible.protocol.union.dto.ActivityTypeDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.UserActivityTypeDto
import com.rarible.protocol.union.dto.parser.IdParser
import com.rarible.protocol.union.core.model.ElasticActivityFilter
import com.rarible.protocol.union.core.model.ElasticActivityQueryGenericFilter
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class ActivityFilterConverter(
    private val userActivityTypeConverter: UserActivityTypeConverter,
    private val featureFlagsProperties: FeatureFlagsProperties,
) {

    fun convertGetAllActivities(
        type: List<ActivityTypeDto>,
        blockchains: List<BlockchainDto>?,
        cursor: String?,
    ): ElasticActivityFilter {
        return when (featureFlagsProperties.enableActivityQueriesPerTypeFilter) {
            true -> TODO("To be implemented under ALPHA-276 Epic")
            else -> ElasticActivityQueryGenericFilter(
                blockchains = blockchains?.toSet().orEmpty(),
                activityTypes = type.toSet(),
                cursor = cursor,
            )
        }
    }

    fun convertGetActivitiesByCollection(
        type: List<ActivityTypeDto>,
        collection: String,
        cursor: String?,
    ): ElasticActivityFilter {
        val collectionId = IdParser.parseCollectionId(collection)
        return when (featureFlagsProperties.enableActivityQueriesPerTypeFilter) {
            true -> TODO("To be implemented under ALPHA-276 Epic")
            else -> ElasticActivityQueryGenericFilter(
                blockchains = setOf(collectionId.blockchain),
                activityTypes = type.toSet(),
                collections = setOf(collectionId.value),
                cursor = cursor,
            )
        }
    }

    fun convertGetActivitiesByItem(
        type: List<ActivityTypeDto>,
        itemId: String,
        cursor: String?,
    ): ElasticActivityFilter {
        val fullItemId = IdParser.parseItemId(itemId)
        return when (featureFlagsProperties.enableActivityQueriesPerTypeFilter) {
            true -> TODO("To be implemented under ALPHA-276 Epic")
            else -> ElasticActivityQueryGenericFilter(
                blockchains = setOf(fullItemId.blockchain),
                activityTypes = type.toSet(),
                item = fullItemId.value,
                cursor = cursor,
            )
        }
    }

    fun convertGetActivitiesByUser(
        type: List<UserActivityTypeDto>,
        user: List<String>,
        blockchains: List<BlockchainDto>?,
        from: Instant?,
        to: Instant?,
        cursor: String?,
    ): ElasticActivityFilter {
        return when (featureFlagsProperties.enableActivityQueriesPerTypeFilter) {
            true -> TODO("To be implemented under ALPHA-276 Epic")
            else -> ElasticActivityQueryGenericFilter(
                blockchains = blockchains?.toSet().orEmpty(),
                activityTypes = type.map { userActivityTypeConverter.convert(it).activityTypeDto }.toSet(), // isMaker is ignored for now
                anyUsers = user.map { IdParser.parseAddress(it).value }.toSet(),
                from = from,
                to = to,
                cursor = cursor,
            )
        }
    }
}
