package com.rarible.protocol.union.dto.continuation

import com.rarible.protocol.union.dto.OrderDto

object OrderContinuation {

    object ByLastUpdatedAndId : ContinuationFactory<OrderDto, DateIdContinuation> {
        override fun getContinuation(entity: OrderDto): DateIdContinuation {
            return DateIdContinuation(entity.lastUpdatedAt, entity.id.value)
        }
    }

    object BySellPriceAndIdAsc : ContinuationFactory<OrderDto, PriceIdContinuation> {
        override fun getContinuation(entity: OrderDto): PriceIdContinuation {
            return PriceIdContinuation(entity.makePrice, entity.id.value, true)
        }
    }

    object ByBidPriceAndIdDesc : ContinuationFactory<OrderDto, PriceIdContinuation> {
        override fun getContinuation(entity: OrderDto): PriceIdContinuation {
            return PriceIdContinuation(entity.takePrice, entity.id.value, false)
        }
    }

    object BySellPriceUsdAndIdAsc : ContinuationFactory<OrderDto, PriceIdContinuation> {
        override fun getContinuation(entity: OrderDto): PriceIdContinuation {
            return PriceIdContinuation(entity.makePriceUsd, entity.id.value, true)
        }
    }

    object ByBidPriceUsdAndIdDesc : ContinuationFactory<OrderDto, PriceIdContinuation> {
        override fun getContinuation(entity: OrderDto): PriceIdContinuation {
            return PriceIdContinuation(entity.takePriceUsd, entity.id.value, false)
        }
    }
}