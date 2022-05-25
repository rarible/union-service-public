package com.rarible.protocol.union.integration.tezos.dipdup.service

import com.rarible.protocol.union.dto.ActivityDto
import com.rarible.protocol.union.dto.ActivitySortDto
import com.rarible.protocol.union.dto.ActivityTypeDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.continuation.page.Slice
import com.rarible.protocol.union.integration.tezos.dipdup.converter.TzktActivityConverter
import com.rarible.tzkt.client.TokenActivityClient

class TzktItemActivityServiceImpl(
    val tzktTokenClient: TokenActivityClient
) : TzktItemActivityService {

    override fun enabled() = true
    private val blockchain = BlockchainDto.TEZOS

    override suspend fun getAll(
        types: List<ActivityTypeDto>,
        continuation: String?,
        limit: Int,
        sort: ActivitySortDto?
    ): Slice<ActivityDto> {
        val tzktTypes = TzktActivityConverter.convertToTzktTypes(types)
        val sortAsc = when (sort) {
            ActivitySortDto.EARLIEST_FIRST -> true
            else -> false
        }
        val page = tzktTokenClient.activities(limit, continuation, sortAsc, tzktTypes)
        return Slice(
            continuation = page.continuation,
            entities = page.items.map { TzktActivityConverter.convert(it, blockchain) }
        )
    }

    override suspend fun getByIds(ids: List<String>): List<ActivityDto> {
        return tzktTokenClient.activityByIds(ids).map { TzktActivityConverter.convert(it, blockchain) }
    }

}
