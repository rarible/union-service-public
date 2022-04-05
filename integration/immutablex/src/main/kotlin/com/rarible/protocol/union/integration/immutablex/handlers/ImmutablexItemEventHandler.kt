package com.rarible.protocol.union.integration.immutablex.handlers

import com.rarible.protocol.union.core.handler.AbstractBlockchainEventHandler
import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.core.model.UnionItem
import com.rarible.protocol.union.core.model.UnionItemDeleteEvent
import com.rarible.protocol.union.core.model.UnionItemEvent
import com.rarible.protocol.union.core.model.UnionItemUpdateEvent
import com.rarible.protocol.union.dto.*
import com.rarible.protocol.union.integration.immutablex.dto.ImmutablexEvent
import com.rarible.protocol.union.integration.immutablex.dto.ImmutablexMint
import com.rarible.protocol.union.integration.immutablex.dto.ImmutablexTransfer
import scalether.domain.Address
import java.math.BigDecimal
import java.math.BigInteger

class ImmutablexItemEventHandler(
    override val handler: IncomingEventHandler<UnionItemEvent>,
): AbstractBlockchainEventHandler<ImmutablexEvent, UnionItemEvent>(BlockchainDto.IMMUTABLEX) {

    private fun toBasePoints(v: BigDecimal): Int {
        return v.multiply(BigDecimal(10000)).toInt()
    }

    override suspend fun handle(event: ImmutablexEvent) {
        val itemEvent = when (event) {
            is ImmutablexMint -> {
                val itemId =
                    ItemIdDto(blockchain, event.token.data.tokenAddress!!, event.token.data.tokenId!!.toBigInteger())
                UnionItemUpdateEvent(
                    itemId = itemId,
                    item = UnionItem(
                        id = itemId,
                        collection = CollectionIdDto(blockchain, event.token.data.tokenAddress),
                        creators = listOf(
                            CreatorDto(
                                account = UnionAddress(BlockchainGroupDto.IMMUTABLEX, event.user),
                                value = 1
                            )
                        ),
                        owners = listOf(
                            UnionAddress(BlockchainGroupDto.IMMUTABLEX, event.user)
                        ),
                        royalties = event.fees?.map {
                            RoyaltyDto(
                                account = UnionAddress(BlockchainGroupDto.IMMUTABLEX, it.address),
                                value = toBasePoints(it.percentage)
                            )
                        } ?: emptyList(),
                        lazySupply = BigInteger.ZERO,
                        supply = BigInteger.ONE,
                        mintedAt = event.timestamp,
                        lastUpdatedAt = event.timestamp,
                        deleted = false
                    )
                )
            }
            is ImmutablexTransfer -> {
                if (event.user == Address.ZERO().hex()) {
                    val itemId =
                        ItemIdDto(blockchain, event.token.data.tokenAddress!!, event.token.data.tokenId!!.toBigInteger())
                    UnionItemDeleteEvent(itemId)
                } else null

            }
            else -> null
        }

        if (itemEvent != null) {
            handler.onEvent(itemEvent)
        }
    }
}
