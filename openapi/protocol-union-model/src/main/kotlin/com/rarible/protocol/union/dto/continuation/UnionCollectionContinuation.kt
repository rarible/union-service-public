package com.rarible.protocol.union.dto.continuation

import com.rarible.protocol.union.dto.UnionCollectionDto

object UnionCollectionContinuation {

    object ById : ContinuationFactory<UnionCollectionDto, IdContinuation> {
        override fun getContinuation(collection: UnionCollectionDto): IdContinuation {
            return IdContinuation(collection.id.value)
        }
    }
}