package com.rarible.protocol.union.worker.job.collection

data class CustomCollectionJobState(
    val rule: Int,
    val state: String
) {

    constructor(state: String) : this(
        state.substringBefore("_").toInt(),
        state.substringAfter("_")
    )

    override fun toString(): String {
        return "${rule}_${state}"
    }
}
