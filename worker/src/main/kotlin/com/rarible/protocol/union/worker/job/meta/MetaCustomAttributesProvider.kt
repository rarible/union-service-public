package com.rarible.protocol.union.worker.job.meta

interface MetaCustomAttributesProvider {

    val name: String
    suspend fun getCustomAttributes(): List<MetaCustomAttributes>

}
