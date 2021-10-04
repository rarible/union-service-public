package com.rarible.protocol.union.enrichment.event

interface ItemEventListener {

    suspend fun onEvent(event: ItemEvent)

}