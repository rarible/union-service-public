package com.rarible.protocol.union.integration.tezos.dipdup.service

import com.rarible.core.logging.Logger
import com.rarible.dipdup.client.OrderActivityClient
import com.rarible.protocol.union.dto.ActivityDto
import com.rarible.protocol.union.dto.ActivitySortDto
import com.rarible.protocol.union.dto.ActivityTypeDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.continuation.page.Slice
import com.rarible.protocol.union.integration.tezos.dipdup.converter.DipDupActivityConverter
import java.math.BigInteger

class DipdupOrderActivityServiceImpl(
    private val dipdupActivityClient: OrderActivityClient,
    private val dipDupActivityConverter: DipDupActivityConverter
): DipdupOrderActivityService {

    override fun enabled() = true

    private val blockchain = BlockchainDto.TEZOS

    override suspend fun getAll(types: List<ActivityTypeDto>, continuation: String?, limit: Int, sort: ActivitySortDto?): Slice<ActivityDto> {
        val dipdupTypes = dipDupActivityConverter.convertToDipDupTypes(types)
        val sortAsc = when (sort) {
            ActivitySortDto.EARLIEST_FIRST -> true
            else -> false
        }
        logger.info("Fetch dipdup all activities: $types, $continuation, $limit, $sort")
        val page = dipdupActivityClient.getActivitiesAll(dipdupTypes, limit, continuation, sortAsc)
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
    ): Slice<ActivityDto> {
        val dipdupTypes = dipDupActivityConverter.convertToDipDupTypes(types)
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

    override suspend fun getByIds(ids: List<String>): List<ActivityDto> {
        logger.info("Fetch dipdup activities by ids: $ids")
        val activities = dipdupActivityClient.getActivitiesByIds(ids)
        return activities.map { dipDupActivityConverter.convert(it, BlockchainDto.TEZOS) }
    }

    companion object {
        private val logger by Logger()
    }

}
