package com.rarible.protocol.union.dto.continuation

import com.rarible.protocol.union.dto.EthCollectionDto
import com.rarible.protocol.union.dto.FlowCollectionDto
import com.rarible.protocol.union.dto.UnionCollectionDto

object UnionCollectionContinuation {

    object ById : ContinuationFactory<UnionCollectionDto, IdContinuation> {
        override fun getContinuation(collection: UnionCollectionDto): IdContinuation {
            return IdContinuation(getId(collection))
        }
    }

    private fun getId(collection: UnionCollectionDto): String {
        return when (collection) {
            is EthCollectionDto -> collection.id.value
            is FlowCollectionDto -> collection.id.value
        }
    }

}