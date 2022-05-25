package com.rarible.protocol.union.enrichment.repository.search

interface EsRepository {
    suspend fun refresh()
    suspend fun deleteAll()
}