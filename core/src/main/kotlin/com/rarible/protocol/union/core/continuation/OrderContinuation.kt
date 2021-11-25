package com.rarible.protocol.union.core.continuation

import com.rarible.protocol.union.dto.OrderDto
import com.rarible.protocol.union.dto.ext

object OrderContinuation {

    object ByLastUpdatedAndId : ContinuationFactory<OrderDto, DateIdContinuation> {
        override fun getContinuation(entity: OrderDto): DateIdContinuation {
            return DateIdContinuation(entity.lastUpdatedAt, entity.id.value)
        }
    }


    object BySellPriceUsdAndIdAsc : ContinuationFactory<OrderDto, UsdPriceIdContinuation> {
        override fun getContinuation(entity: OrderDto): UsdPriceIdContinuation {
            return UsdPriceIdContinuation(
                entity.take.type.ext.currencyAddress(),
                entity.makePrice,
                entity.makePriceUsd,
                entity.id.value,
                true
            )
        }
    }

    object ByBidPriceUsdAndIdDesc : ContinuationFactory<OrderDto, UsdPriceIdContinuation> {
        override fun getContinuation(entity: OrderDto): UsdPriceIdContinuation {
            return UsdPriceIdContinuation(
                entity.make.type.ext.currencyAddress(),
                entity.takePrice,
                entity.takePriceUsd,
                entity.id.value,
                false
            )
        }
    }
}