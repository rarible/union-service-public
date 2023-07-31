package com.rarible.protocol.union.core.continuation

import com.rarible.protocol.union.core.model.UnionCollection
import com.rarible.protocol.union.dto.continuation.ContinuationFactory
import com.rarible.protocol.union.dto.continuation.IdContinuation

object UnionCollectionContinuation {
    object ById : ContinuationFactory<UnionCollection, IdContinuation> {
        override fun getContinuation(entity: UnionCollection): IdContinuation {
            return IdContinuation(entity.id.value)
        }
    }
}
