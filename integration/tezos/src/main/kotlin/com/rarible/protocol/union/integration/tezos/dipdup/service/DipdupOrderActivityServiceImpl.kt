package com.rarible.protocol.union.integration.tezos.dipdup.service

import com.rarible.core.logging.Logger
import com.rarible.dipdup.client.OrderActivityClient
import com.rarible.protocol.union.core.model.UnionActivity
import com.rarible.protocol.union.dto.ActivitySortDto
import com.rarible.protocol.union.dto.ActivityTypeDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.SyncSortDto
import com.rarible.protocol.union.dto.continuation.page.Slice
import com.rarible.protocol.union.integration.tezos.dipdup.converter.DipDupActivityConverter
import java.math.BigInteger

class DipdupOrderActivityServiceImpl(
    private val dipdupActivityClient: OrderActivityClient,
    private val dipDupActivityConverter: DipDupActivityConverter
): DipdupOrderActivityService {

    private val blockchain = BlockchainDto.TEZOS

    override suspend fun getAll(
        types: List<ActivityTypeDto>,
        continuation: String?,
        limit: Int,
        sort: ActivitySortDto?
    ): Slice<UnionActivity> {
        val dipdupTypes = dipDupActivityConverter.convertToDipDupOrderActivitiesTypes(types)
        val sortAsc = when (sort) {
            ActivitySortDto.EARLIEST_FIRST -> true
            else -> false
        }
        return if (dipdupTypes.size > 0) {
            logger.info("Fetch dipdup all order activities: $types, $continuation, $limit, $sort")
            val page = dipdupActivityClient.getActivitiesAll(dipdupTypes, limit, continuation, sortAsc)
            Slice(
                continuation = page.continuation,
                entities = page.activities.map { dipDupActivityConverter.convert(it, blockchain) }
            )
        } else Slice.empty()
    }

    override suspend fun getSync(
        continuation: String?,
        limit: Int,
        sort: SyncSortDto?
    ): Slice<UnionActivity> {
        val sortTezos = sort?.let { DipDupActivityConverter.convert(it) }
        logger.info("Fetch dipdup all order activities sync: $continuation, $limit, $sort")
        val page = dipdupActivityClient.getActivitiesSync(limit, continuation, sortTezos)
        return Slice(
            continuation = page.continuation,
            entities = page.activities.map { dipDupActivityConverter.convert(it, blockchain) }
        )
    }

    override suspend fun getByItem(
        types: List<ActivityTypeDto>,
        contract: String,
        tokenId: BigInteger,
        continuation: String?,
        limit: Int,
        sort: ActivitySortDto?
    ): Slice<UnionActivity> {
        val dipdupTypes = dipDupActivityConverter.convertToDipDupOrderActivitiesTypes(types)
        val sortAsc = when (sort) {
            ActivitySortDto.EARLIEST_FIRST -> true
            else -> false
        }
        return if (dipdupTypes.size > 0) {
            logger.info("Fetch dipdup activities by item: $types, $contract, $tokenId, $continuation, $limit, $sort")
            val page = dipdupActivityClient.getActivitiesByItem(
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

    override suspend fun getByIds(ids: List<String>): List<UnionActivity> {
        logger.info("Fetch dipdup activities by ids: $ids")
        val activities = dipdupActivityClient.getActivitiesByIds(ids)
        return activities.map { dipDupActivityConverter.convert(it, BlockchainDto.TEZOS) }
    }

    companion object {
        private val logger by Logger()
    }

}
