package com.rarible.protocol.union.dto.continuation

import com.rarible.protocol.union.dto.CollectionDto

object UnionCollectionContinuation {

    object ById : ContinuationFactory<CollectionDto, IdContinuation> {
        override fun getContinuation(collection: CollectionDto): IdContinuation {
            return IdContinuation(collection.id.value)
        }
    }
}