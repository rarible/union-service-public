package com.rarible.protocol.union.integration.tezos.dipdup.service

import com.rarible.dipdup.client.OrderActivityClient
import com.rarible.protocol.union.dto.ActivityDto
import com.rarible.protocol.union.dto.ActivitySortDto
import com.rarible.protocol.union.dto.ActivityTypeDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.continuation.page.Slice
import com.rarible.protocol.union.integration.tezos.dipdup.converter.DipDupActivityConverter

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
        val page = dipdupActivityClient.getActivities(dipdupTypes, limit, continuation, sortAsc)
        return Slice(
            continuation = page.continuation,
            entities = page.activities.map { dipDupActivityConverter.convert(it, blockchain) }
        )
    }

    override suspend fun getByIds(ids: List<String>): List<ActivityDto> {
        val activities = dipdupActivityClient.getActivities(ids)
        return activities.map { dipDupActivityConverter.convert(it, BlockchainDto.TEZOS) }
    }

}