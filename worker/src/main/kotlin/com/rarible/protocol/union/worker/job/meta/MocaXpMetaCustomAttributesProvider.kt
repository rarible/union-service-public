package com.rarible.protocol.union.worker.job.meta

import com.fasterxml.jackson.core.json.JsonReadFeature
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.rarible.protocol.union.core.UnionWebClientCustomizer
import com.rarible.protocol.union.core.client.WebClientFactory
import com.rarible.protocol.union.core.model.UnionMetaAttribute
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.parser.IdParser
import com.rarible.protocol.union.worker.config.MocaXpCustomAttributesProviderProperties
import kotlinx.coroutines.reactor.awaitSingle
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import java.math.BigDecimal
import java.math.RoundingMode

@Component
@ConditionalOnProperty("worker.itemMetaCustomAttributesJob.providers.mocaXp.enabled", havingValue = "true")
class MocaXpMetaCustomAttributesProvider(
    properties: MocaXpCustomAttributesProviderProperties,
    private val clientCustomizer: UnionWebClientCustomizer
) : MetaCustomAttributesProvider {

    private val logger = LoggerFactory.getLogger(javaClass)

    private val webClient = createClient(properties.baseUrl, properties.apiKey)
    private val uri = properties.uri
    private val collectionId = IdParser.parseCollectionId(properties.collection)

    override val name = "MocaXP"

    override suspend fun getCustomAttributes(): List<MetaCustomAttributes> {
        logger.info("Fetching mocaXp Item meta custom attributes by URI: {}", uri)
        val json = webClient.get().uri(uri)
            .retrieve()
            .toEntity(String::class.java)
            .awaitSingle()
            .body!!

        val result = MocaXpCustomAttributesParser.parse(json, collectionId)
        logger.info("Fetched mocaXp Item meta custom attributes for {} Items", result.size)
        return result
    }

    private fun createClient(baseUrl: String, apiKey: String?): WebClient {
        val headers = if (apiKey.isNullOrBlank()) {
            emptyMap()
        } else {
            mapOf("x-api-key" to apiKey)
        }

        val builder = WebClientFactory.createClient(baseUrl, headers)
        clientCustomizer.customize(builder)
        return builder.build()
    }
}

object MocaXpCustomAttributesParser {

    private const val FIELD_TRIBE = "tribe"
    private const val FIELD_XP = "total_xp"
    private const val FIELD_TOKEN_ID = "moca_id"

    private const val ATTRIBUTE_XP_PERCENTAGE = "total_xp_percentage"

    private val hundred = BigDecimal("100")

    private val mapper = ObjectMapper().registerKotlinModule()
        .enable(JsonReadFeature.ALLOW_TRAILING_COMMA.mappedFeature())

    fun parse(json: String, collectionId: CollectionIdDto): List<MetaCustomAttributes> {
        val array = mapper.readTree(json) as ArrayNode

        var collectionXp = BigDecimal(0)
        val parsed = array.map {
            val mocaXpAttributes = MocaXpAttributes(it, collectionId)
            collectionXp = mocaXpAttributes.xp?.add(collectionXp) ?: collectionXp
            mocaXpAttributes
        }

        return parsed.map {
            MetaCustomAttributes(
                id = it.id,
                attributes = it.toAttributes(collectionXp)
            )
        }
    }

    private fun toAttributes(node: JsonNode): List<UnionMetaAttribute> {
        return listOfNotNull(
            //node.get(FIELD_TRIBE)?.textValue()?.let { UnionMetaAttribute(FIELD_TRIBE, it) },
            node.get(FIELD_XP)?.decimalValue()?.let { UnionMetaAttribute(FIELD_XP, it.toPlainString()) }
        )
    }

    private fun percentage(collectionXp: BigDecimal, itemXp: BigDecimal): BigDecimal {
        return itemXp.multiply(hundred).divide(collectionXp, 3, RoundingMode.HALF_UP)
    }

    private data class MocaXpAttributes(
        val id: ItemIdDto,
        val xp: BigDecimal?,
        val tribe: String?
    ) {

        constructor(node: JsonNode, collectionId: CollectionIdDto) : this(
            id = ItemIdDto(collectionId.blockchain, "${collectionId.value}:${node.get(FIELD_TOKEN_ID).textValue()}"),
            xp = node.get(FIELD_XP)?.decimalValue(),
            tribe = node.get(FIELD_TRIBE)?.asText()
        )

        fun toAttributes(collectionXp: BigDecimal): List<UnionMetaAttribute> {
            return listOfNotNull(
                //tribe?.let { UnionMetaAttribute(FIELD_TRIBE, it) },
                xp?.let { UnionMetaAttribute(FIELD_XP, it.toPlainString()) },
                xp?.let { UnionMetaAttribute(ATTRIBUTE_XP_PERCENTAGE, percentage(collectionXp, it).toPlainString()) }
            )
        }
    }

}