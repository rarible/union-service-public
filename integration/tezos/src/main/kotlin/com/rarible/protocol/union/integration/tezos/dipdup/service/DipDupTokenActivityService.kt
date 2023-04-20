package com.rarible.protocol.union.integration.tezos.dipdup.service

import com.rarible.dipdup.client.TokenActivityClient
import com.rarible.dipdup.client.model.DipDupSyncSort
import com.rarible.protocol.union.core.model.UnionActivity
import com.rarible.protocol.union.dto.ActivitySortDto
import com.rarible.protocol.union.dto.ActivityTypeDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.SyncSortDto
import com.rarible.protocol.union.dto.continuation.page.Slice
import com.rarible.protocol.union.integration.tezos.dipdup.converter.DipDupActivityConverter
import org.slf4j.LoggerFactory
import java.math.BigInteger

class DipDupTokenActivityService(
    private val dipDupTokenActivityClient: TokenActivityClient,
    private val dipDupActivityConverter: DipDupActivityConverter
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    private val blockchain = BlockchainDto.TEZOS

    suspend fun getAll(
        types: List<ActivityTypeDto>,
        continuation: String?,
        limit: Int,
        sort: ActivitySortDto?
    ): Slice<UnionActivity> {
        val dipdupTypes = dipDupActivityConverter.convertToDipDupNftActivitiesTypes(types)
        val sortAsc = when (sort) {
            ActivitySortDto.EARLIEST_FIRST -> true
            else -> false
        }
        return if (dipdupTypes.size > 0) {
            logger.info("Fetch dipdup all token activities: $types, $continuation, $limit, $sort")
            val page = dipDupTokenActivityClient.getActivitiesAll(dipdupTypes, limit, continuation, sortAsc)
            Slice(
                continuation = page.continuation,
                entities = page.activities.map { dipDupActivityConverter.convert(it, blockchain) }
            )
        } else Slice.empty()
    }

    suspend fun getSync(continuation: String?, limit: Int, sort: SyncSortDto?): Slice<UnionActivity> {
        val sortInternal = sort?.let { DipDupActivityConverter.convert(it) } ?: DipDupSyncSort.DB_UPDATE_DESC
        logger.info("Fetch dipdup all token activities sync: $continuation, $limit, $sort")
        val page = dipDupTokenActivityClient.getActivitiesSync(limit, continuation, sortInternal)
        return Slice(
            continuation = page.continuation,
            entities = page.activities.map { dipDupActivityConverter.convert(it, blockchain) }
        )
    }

    suspend fun getByItem(
        types: List<ActivityTypeDto>,
        contract: String,
        tokenId: BigInteger,
        continuation: String?,
        limit: Int,
        sort: ActivitySortDto?
    ): Slice<UnionActivity> {
        val dipdupTypes = dipDupActivityConverter.convertToDipDupNftActivitiesTypes(types)
        val sortAsc = when (sort) {
            ActivitySortDto.EARLIEST_FIRST -> true
            else -> false
        }
        return if (dipdupTypes.size > 0) {
            logger.info("Fetch dipdup activities by item: $types, $contract, $tokenId, $continuation, $limit, $sort")
            val page = dipDupTokenActivityClient.getActivitiesByItem(
                dipdupTypes,
                contract,
                tokenId.toString(),
                limit,
                continuation,
                sortAsc
            )
            Slice(
                continuation = page.continuation,
                entities = page.activities.map { dipDupActivityConverter.convert(it, blockchain) }
            )
        } else {
            return Slice.empty()
        }
    }

    suspend fun getByIds(ids: List<String>): List<UnionActivity> {
        logger.info("Fetch dipdup activities by ids: $ids")
        val activities = dipDupTokenActivityClient.getActivitiesByIds(ids)
        return activities.map { dipDupActivityConverter.convert(it, BlockchainDto.TEZOS) }
    }

}
