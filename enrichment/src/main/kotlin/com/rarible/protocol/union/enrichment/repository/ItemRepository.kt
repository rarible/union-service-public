package com.rarible.protocol.union.enrichment.repository

import com.mongodb.client.result.DeleteResult
import com.rarible.protocol.union.dto.AuctionIdDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.PlatformDto
import com.rarible.protocol.union.enrichment.model.ShortItem
import com.rarible.protocol.union.enrichment.model.ShortItemId
import kotlinx.coroutines.flow.Flow
import java.time.Instant

interface ItemRepository {

    suspend fun createIndices()

    suspend fun save(item: ShortItem): ShortItem

    suspend fun get(id: ShortItemId): ShortItem?

    suspend fun getAll(ids: List<ShortItemId>): List<ShortItem>

    fun findWithMultiCurrency(lastUpdateAt: Instant): Flow<ShortItem>
    fun findByBlockchain(fromShortItemId: ShortItemId?, blockchain: BlockchainDto?, limit: Int): Flow<ShortItem>
    fun findByAuction(auctionId: AuctionIdDto): Flow<ShortItem>
    fun findByPlatformWithSell(platform: PlatformDto, fromShortItemId: ShortItemId?): Flow<ShortItem>

    suspend fun delete(itemId: ShortItemId): DeleteResult?
}