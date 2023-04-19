package com.rarible.protocol.union.core.continuation

import com.rarible.protocol.union.core.model.UnionActivityDto
import com.rarible.protocol.union.dto.continuation.ContinuationFactory
import com.rarible.protocol.union.dto.continuation.DateContinuation
import com.rarible.protocol.union.dto.continuation.DateIdContinuation

object UnionActivityContinuation {

    object ByLastUpdatedAndIdDesc : ContinuationFactory<UnionActivityDto, DateIdContinuation> {

        override fun getContinuation(entity: UnionActivityDto): DateIdContinuation {
            return DateIdContinuation(
                entity.date,
                entity.id.value,
                false
            )
        }
    }

    object ByLastUpdatedAndIdAsc : ContinuationFactory<UnionActivityDto, DateIdContinuation> {

        override fun getContinuation(entity: UnionActivityDto): DateIdContinuation {
            return DateIdContinuation(
                entity.date,
                entity.id.value,
                true
            )
        }
    }

    object ByLastUpdatedSyncAndIdDesc : ContinuationFactory<UnionActivityDto, DateIdContinuation> {

        override fun getContinuation(entity: UnionActivityDto): DateIdContinuation {
            return DateIdContinuation(
                entity.lastUpdatedAt ?: entity.date,
                entity.id.value,
                false
            )
        }
    }

    object ByLastUpdatedSyncAndIdAsc : ContinuationFactory<UnionActivityDto, DateIdContinuation> {

        override fun getContinuation(entity: UnionActivityDto): DateIdContinuation {
            return DateIdContinuation(
                entity.lastUpdatedAt ?: entity.date,
                entity.id.value,
                true
            )
        }
    }

    object ByLastUpdatedDesc : ContinuationFactory<UnionActivityDto, DateContinuation> {

        override fun getContinuation(entity: UnionActivityDto): DateContinuation {
            return DateContinuation(
                entity.date,
                entity.id.value,
                false
            )
        }
    }

    object ByLastUpdatedAsc : ContinuationFactory<UnionActivityDto, DateContinuation> {

        override fun getContinuation(entity: UnionActivityDto): DateContinuation {
            return DateContinuation(
                entity.date,
                entity.id.value,
                true
            )
        }
    }
}

