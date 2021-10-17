package com.rarible.protocol.union.enrichment.event

interface CollectionEventListener {

    suspend fun onEvent(event: CollectionEvent)

}