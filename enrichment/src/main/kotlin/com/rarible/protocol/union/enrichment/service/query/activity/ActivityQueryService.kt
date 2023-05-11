package com.rarible.protocol.union.enrichment.service.query.activity

import com.rarible.protocol.union.dto.ActivitiesDto
import com.rarible.protocol.union.dto.ActivitySortDto
import com.rarible.protocol.union.dto.ActivityTypeDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.CurrencyIdDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.SyncSortDto
import com.rarible.protocol.union.dto.SyncTypeDto
import com.rarible.protocol.union.dto.UnionAddress
import com.rarible.protocol.union.dto.UserActivityTypeDto
import java.time.Instant

interface ActivityQueryService {

    suspend fun getAllActivities(
        type: List<ActivityTypeDto>,
        blockchains: List<BlockchainDto>?,
        bidCurrencies: List<CurrencyIdDto>?,
        continuation: String?,
        cursor: String?,
        size: Int?,
        sort: ActivitySortDto?
    ): ActivitiesDto

    suspend fun getAllActivitiesSync(
        blockchain: BlockchainDto,
        continuation: String?,
        size: Int?,
        sort: SyncSortDto?,
        type: SyncTypeDto?
    ): ActivitiesDto

    suspend fun getAllRevertedActivitiesSync(
        blockchain: BlockchainDto,
        continuation: String?,
        size: Int?,
        sort: SyncSortDto?,
        type: SyncTypeDto?
    ): ActivitiesDto

    suspend fun getActivitiesByCollection(
        type: List<ActivityTypeDto>,
        collection: List<CollectionIdDto>,
        bidCurrencies: List<CurrencyIdDto>?,
        continuation: String?,
        cursor: String?,
        size: Int?,
        sort: ActivitySortDto?
    ): ActivitiesDto

    suspend fun getActivitiesByItem(
        type: List<ActivityTypeDto>,
        itemId: ItemIdDto,
        bidCurrencies: List<CurrencyIdDto>?,
        continuation: String?,
        cursor: String?,
        size: Int?,
        sort: ActivitySortDto?
    ): ActivitiesDto

    suspend fun getActivitiesByUser(
        type: List<UserActivityTypeDto>,
        user: List<UnionAddress>,
        blockchains: List<BlockchainDto>?,
        bidCurrencies: List<CurrencyIdDto>?,
        from: Instant?,
        to: Instant?,
        continuation: String?,
        cursor: String?,
        size: Int?,
        sort: ActivitySortDto?
    ): ActivitiesDto
}
