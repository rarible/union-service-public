package com.rarible.protocol.union.dto.continuation

import com.rarible.protocol.union.dto.CollectionDto

object UnionCollectionContinuation {

    object ById : ContinuationFactory<CollectionDto, IdContinuation> {
        override fun getContinuation(entity: CollectionDto): IdContinuation {
            return IdContinuation(entity.id.value)
        }
    }
}