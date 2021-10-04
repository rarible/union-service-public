package com.rarible.protocol.union.enrichment.event

interface OwnershipEventListener {

    suspend fun onEvent(event: OwnershipEvent)

}