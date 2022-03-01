package com.rarible.protocol.union.integration.immutablex.service

import com.rarible.core.common.nowMillis
import com.rarible.protocol.union.core.converter.ContractAddressConverter
import com.rarible.protocol.union.core.converter.UnionAddressConverter
import com.rarible.protocol.union.core.model.UnionItem
import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.core.service.ItemService
import com.rarible.protocol.union.core.service.router.AbstractBlockchainService
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.RoyaltyDto
import com.rarible.protocol.union.dto.continuation.page.Page
import com.rarible.protocol.union.integration.immutablex.client.ImmutablexApiClient
import com.rarible.protocol.union.integration.immutablex.converter.ImmutablexItemConverter
import com.rarible.protocol.union.integration.immutablex.dto.ImmutablexAsset
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient
import java.math.BigDecimal
import java.math.BigInteger

class ImmutablexItemService(
    private val client: ImmutablexApiClient
): AbstractBlockchainService(BlockchainDto.IMMUTABLEX), ItemService {


    private val zeroAccount = "0x0000000000000000000000000000000000000000"

    override suspend fun getAllItems(
        continuation: String?,
        size: Int,
        showDeleted: Boolean?,
        lastUpdatedFrom: Long?,
        lastUpdatedTo: Long?,
    ): Page<UnionItem> {
        TODO("Not yet implemented")
    }

    override suspend fun getItemById(itemId: String): UnionItem {
        val asset = client.getAsset(itemId)
        return ImmutablexItemConverter.convert(asset, blockchain)

    }

    override suspend fun getItemRoyaltiesById(itemId: String): List<RoyaltyDto> {
        TODO("Not yet implemented")
    }

    override suspend fun getItemMetaById(itemId: String): UnionMeta {
        TODO("Not yet implemented")
    }

    override suspend fun resetItemMeta(itemId: String) {
        TODO("Not yet implemented")
    }

    override suspend fun getItemsByCollection(
        collection: String,
        owner: String?,
        continuation: String?,
        size: Int,
    ): Page<UnionItem> {
        TODO("Not yet implemented")
    }

    override suspend fun getItemsByCreator(creator: String, continuation: String?, size: Int): Page<UnionItem> {
        TODO("Not yet implemented")
    }

    override suspend fun getItemsByOwner(owner: String, continuation: String?, size: Int): Page<UnionItem> {
        TODO("Not yet implemented")
    }

    override suspend fun getItemsByIds(itemIds: List<String>): List<UnionItem> {
        TODO("Not yet implemented")
    }
}
