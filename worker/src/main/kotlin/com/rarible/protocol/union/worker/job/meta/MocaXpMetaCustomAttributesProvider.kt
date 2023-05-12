package com.rarible.protocol.union.worker.job.meta

import com.fasterxml.jackson.core.json.JsonReadFeature
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.rarible.protocol.union.core.client.WebClientFactory
import com.rarible.protocol.union.core.model.UnionMetaAttribute
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.worker.config.MocaXpCustomAttributesProviderProperties
import kotlinx.coroutines.reactor.awaitSingle
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import java.net.URI

@Component
@ConditionalOnProperty("worker.itemMetaCustomAttributesJob.providers.mocaXp.enabled", havingValue = "true")
class MocaXpMetaCustomAttributesProvider(
    properties: MocaXpCustomAttributesProviderProperties
) : MetaCustomAttributesProvider {

    private val logger = LoggerFactory.getLogger(javaClass)

    private val client = createClient(properties.baseUrl, properties.apiKey)
    private val uri = URI(properties.uri)

    override val name = "MocaXP"

    override suspend fun getCustomAttributes(): List<MetaCustomAttributes> {
        logger.info("Fetching mocaXp Item meta custom attributes by URI: {}", uri)
        val json = client.get().uri(uri)
            .retrieve()
            .toEntity(String::class.java)
            .awaitSingle()
            .body!!

        val result = MocaXpCustomAttributesParser.parse(json)
        logger.info("Fetched mocaXp Item meta custom attributes for {} Items", result.size)
        return result
    }

    private fun createClient(baseUrl: String, apiKey: String?): WebClient {
        val headers = apiKey?.let { mapOf("x-api-key" to it) } ?: emptyMap()
        return WebClientFactory.createClient(baseUrl, headers).build()
    }
}

object MocaXpCustomAttributesParser {

    private const val FIELD_TRIBE = "tribe"
    private const val FIELD_XP = "total_xp"
    private const val FIELD_TOKEN_ID = "moca_id"
    private const val COLLECTION_ID = "0x59325733eb952a92e069c87f0a6168b29e80627f"

    private val mapper = ObjectMapper().registerKotlinModule()
        .enable(JsonReadFeature.ALLOW_TRAILING_COMMA.mappedFeature())

    fun parse(json: String): List<MetaCustomAttributes> {
        val array = mapper.readTree(json) as ArrayNode

        return array.map { node ->
            MetaCustomAttributes(
                id = getItemId(node),
                attributes = toAttributes(node)
            )
        }
    }

    private fun toAttributes(node: JsonNode): List<UnionMetaAttribute> {
        return listOfNotNull(
            node.get(FIELD_TRIBE)?.textValue()?.let { UnionMetaAttribute(FIELD_TRIBE, it) },
            node.get(FIELD_XP)?.decimalValue()?.let { UnionMetaAttribute(FIELD_XP, it.toPlainString()) }
        )
    }

    private fun getItemId(node: JsonNode): ItemIdDto {
        val tokenId = node.get(FIELD_TOKEN_ID).textValue()
        val itemId = "${COLLECTION_ID}:$tokenId"
        return ItemIdDto(BlockchainDto.ETHEREUM, itemId)
    }
}