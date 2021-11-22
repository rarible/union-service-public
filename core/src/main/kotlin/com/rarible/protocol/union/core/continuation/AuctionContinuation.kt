package com.rarible.protocol.union.core.continuation

import com.rarible.protocol.union.dto.AuctionDto
import com.rarible.protocol.union.dto.ext

object AuctionContinuation {

    object ByLastUpdateDesc : ContinuationFactory<AuctionDto, DateIdContinuation> {
        override fun getContinuation(entity: AuctionDto): DateIdContinuation {
            return DateIdContinuation(entity.lastUpdateAt, entity.id.value)
        }
    }

    object ByLastUpdateAsc : ContinuationFactory<AuctionDto, DateIdContinuation> {
        override fun getContinuation(entity: AuctionDto): DateIdContinuation {
            return DateIdContinuation(entity.lastUpdateAt, entity.id.value, true)
        }
    }

    object ByBuyPriceUsdAsc : ContinuationFactory<AuctionDto, UsdPriceIdContinuation> {
        override fun getContinuation(entity: AuctionDto): UsdPriceIdContinuation {
            return UsdPriceIdContinuation(
                entity.sell.type.ext.contract,
                entity.buyPrice,
                entity.buyPriceUsd,
                entity.id.value,
                true
            )
        }
    }
}
