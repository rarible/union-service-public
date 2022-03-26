package com.rarible.protocol.union.enrichment.configuration

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConstructorBinding
@ConfigurationProperties("meta")
data class UnionMetaProperties(
    val ipfsGateway: String,
    val mediaFetchMaxSize: Long,
    val openSeaProxyUrl: String,
    val httpClient: HttpClient
){
    class HttpClient(
        val name: String = "ktor-apache",
        val threadCount: Int = 8,
        val timeOut: Int = 5000,
        val totalConnection: Int = 500
    )
}
