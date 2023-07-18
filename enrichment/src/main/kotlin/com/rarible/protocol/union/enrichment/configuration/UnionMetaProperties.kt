package com.rarible.protocol.union.enrichment.configuration

import com.rarible.protocol.union.dto.BlockchainDto
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import java.time.Duration

@ConstructorBinding
@ConfigurationProperties("meta")
data class UnionMetaProperties(
    val ipfsGateway: String,
    val ipfsPublicGateway: String,
    val ipfsLegacyGateway: String?,
    val mediaFetchMaxSize: Long,
    val openSeaProxyUrl: String,
    val embedded: EmbeddedContentProperties,
    val trimming: MetaTrimmingProperties = MetaTrimmingProperties(),
    val httpClient: HttpClient = HttpClient(),
    val simpleHash: SimpleHash = SimpleHash(),
    private val retries: String = "" //  TODO not sure it should be here
) {

    val retryIntervals = retries.split(",")
        .filter { it.isNotBlank() }
        .map { Duration.parse(it) }
        .ifEmpty {
            listOf(
                Duration.ofHours(1),
                Duration.ofHours(24)
            )
        }

    class HttpClient(
        val type: HttpClientType = HttpClientType.KTOR_CIO,
        val threadCount: Int = 8,
        val timeOut: Int = 5000,
        val totalConnection: Int = 500,
        val connectionsPerRoute: Int = 20,
        val keepAlive: Boolean = true
    ) {

        enum class HttpClientType {
            KTOR_APACHE,
            KTOR_CIO,
            ASYNC_APACHE
        }
    }
}

data class EmbeddedContentProperties(
    val publicUrl: String,
    @Deprecated("Should be removed after migration")
    val legacyUrls: String = ""
)

data class MetaTrimmingProperties(
    val suffix: String = "...",
    val nameLength: Int = 10000,
    val descriptionLength: Int = 50000,
    val attributesSize: Int = 200,
    val attributeNameLength: Int = 500,
    val attributeValueLength: Int = 2000
)

data class SimpleHash(
    val enabled: Boolean = false,
    val endpoint: String = "https://api.simplehash.com/api/v0",
    val apiKey: String = "",
    val supported: Set<BlockchainDto> = setOf(BlockchainDto.ETHEREUM /* BlockchainDto.POLYGON, BlockchainDto.FLOW*/),

    // this is needed to mapping for test networks
    val mapping: Map<String, String> = emptyMap(),
    val cacheExpiration: Duration = Duration.ofMinutes(10)
)
