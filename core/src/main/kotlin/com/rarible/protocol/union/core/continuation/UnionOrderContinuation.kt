package com.rarible.protocol.union.core.continuation

import com.rarible.protocol.union.core.model.UnionOrder
import com.rarible.protocol.union.dto.continuation.ContinuationFactory
import com.rarible.protocol.union.dto.continuation.DateIdContinuation
import com.rarible.protocol.union.dto.continuation.UsdPriceIdContinuation

object UnionOrderContinuation {

    object ByLastUpdatedAndIdAsc : ContinuationFactory<UnionOrder, DateIdContinuation> {

        override fun getContinuation(entity: UnionOrder): DateIdContinuation {
            return DateIdContinuation(entity.lastUpdatedAt, entity.id.value, true)
        }
    }

    object ByLastUpdatedAndIdDesc : ContinuationFactory<UnionOrder, DateIdContinuation> {

        override fun getContinuation(entity: UnionOrder): DateIdContinuation {
            return DateIdContinuation(entity.lastUpdatedAt, entity.id.value, false)
        }
    }

    object BySellPriceUsdAndIdAsc : ContinuationFactory<UnionOrder, UsdPriceIdContinuation> {

        override fun getContinuation(entity: UnionOrder): UsdPriceIdContinuation {
            return UsdPriceIdContinuation(
                entity.take.type.currencyId()!!,
                entity.makePrice,
                entity.makePriceUsd,
                entity.id.value,
                true
            )
        }
    }

    object ByBidPriceUsdAndIdDesc : ContinuationFactory<UnionOrder, UsdPriceIdContinuation> {

        override fun getContinuation(entity: UnionOrder): UsdPriceIdContinuation {
            return UsdPriceIdContinuation(
                entity.make.type.currencyId()!!,
                entity.takePrice,
                entity.takePriceUsd,
                entity.id.value,
                false
            )
        }
    }
}
