package com.rarible.protocol.union.api.service.elastic

import com.rarible.protocol.union.api.service.UserActivityTypeConverter
import com.rarible.protocol.union.core.model.ActivityByCollectionFilter
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
) {

    fun convertGetAllActivities(
        type: List<ActivityTypeDto>,
        blockchains: List<BlockchainDto>?,
        cursor: String?,
    ): ElasticActivityFilter {
        return ElasticActivityQueryGenericFilter(
            blockchains = blockchains?.toSet().orEmpty(),
            activityTypes = type.toSet(),
            cursor = cursor,
        )
    }

    fun convertGetActivitiesByCollection(
        type: List<ActivityTypeDto>,
        collections: List<String>,
        cursor: String?,
    ): ElasticActivityFilter {
        val cols = collections.map(IdParser::parseCollectionId)
        return ActivityByCollectionFilter(
            activityTypes = type.toSet(),
            collections = cols,
            cursor = cursor,
        )
    }

    fun convertGetActivitiesByItem(
        type: List<ActivityTypeDto>,
        itemId: String,
        cursor: String?,
    ): ElasticActivityFilter {
        val fullItemId = IdParser.parseItemId(itemId)
        return ElasticActivityQueryGenericFilter(
            blockchains = setOf(fullItemId.blockchain),
            activityTypes = type.toSet(),
            item = fullItemId.value,
            cursor = cursor,
        )
    }

    fun convertGetActivitiesByUser(
        type: List<UserActivityTypeDto>,
        user: List<String>,
        blockchains: List<BlockchainDto>?,
        from: Instant?,
        to: Instant?,
        cursor: String?,
    ): ElasticActivityFilter {
        val parsedUsers = user.map { IdParser.parseAddress(it).value }.toSet()
        val (userFilters, activityTypes) = activityTypeByUsers(type, parsedUsers)
        val finalUserFilters = userFilters.normalize()

        return ElasticActivityQueryGenericFilter(
            blockchains = blockchains?.toSet().orEmpty(),
            activityTypes = activityTypes, // isMaker is ignored for now
            anyUsers = finalUserFilters.anyUsers,
            usersFrom = finalUserFilters.fromUsers,
            usersTo = finalUserFilters.toUsers,
            from = from,
            to = to,
            cursor = cursor,
        )
    }

    private fun activityTypeByUsers(
        userActivityTypes: List<UserActivityTypeDto>,
        parsedUsers: Set<String>
    ) = userActivityTypes.foldRight(
        UserFilters.anyUsers(parsedUsers) to emptySet<ActivityTypeDto>()
    ) { userActivityType, (users, convertedTypes) ->
        val convertedType = userActivityTypeConverter.convert(userActivityType).activityTypeDto

        // here we set
        val modifiedUserFilters = when (userActivityType) {
            in listOf(UserActivityTypeDto.SELL, UserActivityTypeDto.TRANSFER_FROM) -> {
                users.copy(fromUsers = parsedUsers, anyUsers = emptySet())
            }
            in listOf(UserActivityTypeDto.BUY, UserActivityTypeDto.TRANSFER_TO) -> {
                users.copy(toUsers = parsedUsers, anyUsers = emptySet())
            }
            else -> users
        }

        modifiedUserFilters to (convertedTypes + convertedType)
    }
}

private data class UserFilters(
    val fromUsers: Set<String>,
    val toUsers: Set<String>,
    val anyUsers: Set<String>
) {

    /**
     * This method forces usage of anyUsers filter in case of mixed activity types are used.
     * E.g. for (SELL + BUY), anyUsers filter is used in ES
     */
    fun normalize(): UserFilters {
        return if(fromUsers == toUsers && fromUsers.isNotEmpty()) this.copy(anyUsers = fromUsers, fromUsers = emptySet(), toUsers = emptySet())
        else this
    }
    companion object {
        fun anyUsers(users: Set<String>): UserFilters {
            return UserFilters(emptySet(), emptySet(), users)
        }
    }
}
