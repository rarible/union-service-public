package com.rarible.protocol.union.core.continuation

import com.rarible.protocol.union.core.model.UnionActivity
import com.rarible.protocol.union.dto.continuation.ContinuationFactory
import com.rarible.protocol.union.dto.continuation.DateContinuation
import com.rarible.protocol.union.dto.continuation.DateIdContinuation

object UnionActivityContinuation {

    object ByLastUpdatedAndIdDesc : ContinuationFactory<UnionActivity, DateIdContinuation> {

        override fun getContinuation(entity: UnionActivity): DateIdContinuation {
            return DateIdContinuation(
                entity.date,
                entity.id.value,
                false
            )
        }
    }

    object ByLastUpdatedAndIdAsc : ContinuationFactory<UnionActivity, DateIdContinuation> {

        override fun getContinuation(entity: UnionActivity): DateIdContinuation {
            return DateIdContinuation(
                entity.date,
                entity.id.value,
                true
            )
        }
    }

    object ByLastUpdatedSyncAndIdDesc : ContinuationFactory<UnionActivity, DateIdContinuation> {

        override fun getContinuation(entity: UnionActivity): DateIdContinuation {
            return DateIdContinuation(
                entity.lastUpdatedAt ?: entity.date,
                entity.id.value,
                false
            )
        }
    }

    object ByLastUpdatedSyncAndIdAsc : ContinuationFactory<UnionActivity, DateIdContinuation> {

        override fun getContinuation(entity: UnionActivity): DateIdContinuation {
            return DateIdContinuation(
                entity.lastUpdatedAt ?: entity.date,
                entity.id.value,
                true
            )
        }
    }

    object ByLastUpdatedDesc : ContinuationFactory<UnionActivity, DateContinuation> {

        override fun getContinuation(entity: UnionActivity): DateContinuation {
            return DateContinuation(
                entity.date,
                entity.id.value,
                false
            )
        }
    }

    object ByLastUpdatedAsc : ContinuationFactory<UnionActivity, DateContinuation> {

        override fun getContinuation(entity: UnionActivity): DateContinuation {
            return DateContinuation(
                entity.date,
                entity.id.value,
                true
            )
        }
    }
}

