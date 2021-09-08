package com.rarible.protocol.union.dto.continuation

data class IdContinuation(
    val id: String
) : Continuation<IdContinuation> {

    override fun toString(): String {
        return id
    }

    override fun compareTo(other: IdContinuation): Int {
        return this.id.compareTo(other.id)
    }

}
