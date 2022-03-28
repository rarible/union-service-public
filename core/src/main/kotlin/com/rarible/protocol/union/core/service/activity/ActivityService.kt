package com.rarible.service.activity

import com.rarible.core.apm.SpanType
import com.rarible.core.apm.withSpan
import com.rarible.domain.ActivityType
import com.rarible.domain.ResultWithContinuation
import com.rarible.domain.activity.AbstractActivity
import com.rarible.domain.activity.MintActivity
import com.rarible.domain.constant.ActivitySort
import com.rarible.marketplace.core.model.BlockchainAddress
import com.rarible.service.protocol.client.ActivityClient
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class ActivityService(
    private val activityClient: ActivityClient
) {
    suspend fun getMintActivitiesByUser(
        users: List<BlockchainAddress>,
        from: Instant? = null,
        to: Instant? = null,
        sort: ActivitySort? = null
    ): List<MintActivity> = withSpan(name = "ActivityService#getMintActivitiesByUser", type = SpanType.APP) {
        val result = mutableListOf<MintActivity>()

        val types = listOf(ActivityType.MINT)
        var continuation: String? = null
        do {
            val activitiesWithContinuation = getActivitiesByUserInternal(
                users = users,
                types = types,
                from = from,
                to = to,
                continuation = continuation,
                sort = sort
            )
            val mintActivities = activitiesWithContinuation.elements
                .filterIsInstance(MintActivity::class.java)

            result.addAll(mintActivities)

            continuation = activitiesWithContinuation.continuation
        } while (continuation != null)

        result
    }

    private suspend fun getActivitiesByUserInternal(
        users: List<BlockchainAddress>,
        types: List<ActivityType>,
        from: Instant? = null,
        to: Instant? = null,
        continuation: String? = null,
        size: Int? = null,
        sort: ActivitySort? = null
    ): ResultWithContinuation<AbstractActivity> {
        return activityClient.getActivitiesByUser(users, types, from, to, continuation, size, sort)
    }
}
