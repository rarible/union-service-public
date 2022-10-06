package com.rarible.protocol.union.integration.tezos.dipdup.service

import com.rarible.protocol.union.dto.ActivityDto
import com.rarible.protocol.union.dto.ActivitySortDto
import com.rarible.protocol.union.dto.ActivityTypeDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.continuation.page.Slice
import com.rarible.protocol.union.integration.tezos.dipdup.converter.TzktActivityConverter
import com.rarible.tzkt.client.TokenActivityClient
import java.math.BigInteger

class TzktItemActivityServiceImpl(
    val tzktTokenClient: TokenActivityClient
) : TzktItemActivityService {

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
        return if (tzktTypes.size > 0) {
            val page = tzktTokenClient.getActivitiesAll(tzktTypes, limit, continuation, sortAsc)
            Slice(
                continuation = page.continuation,
                entities = page.items.map { TzktActivityConverter.convert(it, blockchain) }
            )
        } else Slice.empty()
    }

    override suspend fun getByItem(
        types: List<ActivityTypeDto>,
        contract: String,
        tokenId: BigInteger,
        continuation: String?,
        limit: Int,
        sort: ActivitySortDto?
    ): Slice<ActivityDto> {
        val tzktTypes = TzktActivityConverter.convertToTzktTypes(types)
        val sortAsc = when (sort) {
            ActivitySortDto.EARLIEST_FIRST -> true
            else -> false
        }
        return if (tzktTypes.size > 0) {
            val page = tzktTokenClient.getActivitiesByItem(
                tzktTypes,
                contract,
                tokenId.toString(),
                limit,
                continuation,
                sortAsc
            )
            Slice(
                continuation = page.continuation,
                entities = page.items.map { TzktActivityConverter.convert(it, blockchain) }
            )
        } else {
            return Slice.empty()
        }
    }

    override suspend fun getByIds(ids: List<String>, wrapHash: Boolean): List<ActivityDto> {
        return tzktTokenClient.getActivitiesByIds(ids, wrapHash).map { TzktActivityConverter.convert(it, blockchain) }
    }

}
