package com.rarible.protocol.union.enrichment.configuration

import com.rarible.core.kafka.Compression
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.enrichment.model.EnrichmentCollectionId
import com.rarible.protocol.union.enrichment.model.ShortItemId
import com.rarible.protocol.union.enrichment.util.TokenRange
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import java.time.Duration

@ConstructorBinding
@ConfigurationProperties(prefix = "enrichment")
class EnrichmentProperties(
    val producer: ProducerProperties,
    val collection: EnrichmentCollectionProperties = EnrichmentCollectionProperties(),
    val currencies: EnrichmentCurrenciesProperties = EnrichmentCurrenciesProperties(),
    val meta: EnrichmentMetaProperties,
)

data class ProducerProperties(
    val brokerReplicaSet: String,
    val compression: Compression = Compression.SNAPPY,
)

data class EnrichmentCurrenciesProperties(
    val bestBidByCurrencyWhitelist: List<String> = emptyList(),
)

data class EnrichmentMetaProperties(
    val common: CommonMetaProperties,
    val item: EnrichmentItemMetaProperties = EnrichmentItemMetaProperties()
    // TODO add for collections if needed
)

// ---------------------- Common Meta -----------------------//

data class CommonMetaProperties(
    val ipfsGateway: String,
    val ipfsPublicGateway: String,
    val ipfsLegacyGateway: String?,
    val alwaysSubstituteIpfsGateway: Boolean = true,
    val mediaFetchMaxSize: Long,
    val embedded: EmbeddedContentProperties,
    val trimming: MetaTrimmingProperties = MetaTrimmingProperties(),
    val httpClient: HttpClient = HttpClient(),
    val simpleHash: SimpleHash = SimpleHash(),
    private val retries: String = "" //  TODO not sure it should be here
) {

    private val defaultRetries = listOf(
        Duration.ofHours(1),
        Duration.ofHours(24)
    )

    val retryIntervals = retries.split(",")
        .filter { it.isNotBlank() }
        .map { Duration.parse(it) }
        .ifEmpty { defaultRetries }

    class HttpClient(
        val type: HttpClientType = HttpClientType.ASYNC_APACHE,
        val threadCount: Int = 4,
        val timeOut: Int = 30000,
        val totalConnection: Int = 8196,
        val connectionsPerRoute: Int = 2048,
        val keepAlive: Boolean = false
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
)

data class MetaTrimmingProperties(
    val suffix: String = "...",
    val nameLength: Int = 10000,
    val descriptionLength: Int = 50000,
    val attributesSize: Int = 500,
    val attributeNameLength: Int = 500,
    val attributeValueLength: Int = 2000
)

data class SimpleHash(
    val enabled: Boolean = false,
    val endpoint: String = "",
    val apiKey: String = "",
    val supported: Set<BlockchainDto> = emptySet(),
    val supportedCollection: Set<BlockchainDto> = emptySet(),

    // this is needed to mapping for test networks
    val mapping: Map<String, String> = emptyMap(),
    val cacheExpiration: Duration = Duration.ofMinutes(10),

    val kafka: SimpleHashKafka = SimpleHashKafka()
)

data class SimpleHashKafka(
    val enabled: Boolean = false,
    val broker: String = "",
    val concurrency: Int = 1,
    val batchSize: Int = 100,

    // topic depends on environment
    val topics: List<String> = emptyList(),

    val username: String? = null,
    val password: String? = null,
)

// ---------------------- Item Meta -------------------------//

class EnrichmentItemMetaProperties(
    val customizers: EnrichmentItemMetaCustomizerProperties = EnrichmentItemMetaCustomizerProperties(),
    val numberOfItemsToCheckForMetaChanges: Int = 10,
)

class EnrichmentItemMetaCustomizerProperties(
    val mattel: EnrichmentMattelMetaCustomizerProperties = EnrichmentMattelMetaCustomizerProperties()
)

class EnrichmentMattelMetaCustomizerProperties(
    val barbieCard: List<String> = emptyList(),
    val barbieToken: List<String> = emptyList(),
    val barbiePack: List<String> = emptyList(),
    val hwCard: List<String> = emptyList(),
    val hwPack: List<String> = emptyList(),
    val hwToken: List<String> = emptyList(),
)

// ------------------- Custom Collections -------------------//

data class EnrichmentCollectionProperties(
    val mappings: List<CustomCollectionMapping> = emptyList(),
)

// Contains fully-qualified collection/item identifiers
data class CustomCollectionMapping(
    val enabled: Boolean = true,
    // Full ID of collection for default mapping OR name of custom mapper
    val name: String,
    private val items: List<String> = emptyList(),
    private val collections: List<String> = emptyList(),
    private val ranges: List<String> = emptyList(),
    val meta: CustomCollectionMetaMapping = CustomCollectionMetaMapping()
) {

    fun getItemIds(): List<ShortItemId> = items.map { ShortItemId.of(it) }
    fun getCollectionIds(): List<EnrichmentCollectionId> = collections.map { EnrichmentCollectionId.of(it) }
    fun getRanges(): List<TokenRange> = ranges.map { TokenRange.of(it) }
}

data class CustomCollectionMetaMapping(
    private val collections: List<String> = emptyList(),
    val attributes: List<CustomCollectionMetaAttributeMapping> = emptyList()
) {

    fun getCollectionIds(): List<EnrichmentCollectionId> = collections.map { EnrichmentCollectionId.of(it) }
    fun getAttributes(): Map<String, Set<String>> = attributes.associateBy({ it.name }, { it.values })
}

data class CustomCollectionMetaAttributeMapping(
    val name: String,
    val values: Set<String>
)
