package com.rarible.protocol.union.enrichment.meta.simplehash

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.parser.IdParser
import com.rarible.protocol.union.enrichment.meta.MetaSource
import com.rarible.protocol.union.enrichment.model.RawMetaCache
import com.simplehash.v0.nft
import org.apache.commons.text.StringEscapeUtils
import java.time.Instant
import java.time.LocalDateTime

object SimpleHashConverter {

    private val mapper = ObjectMapper().run {
        registerKotlinModule()
        registerModule(JavaTimeModule())
        configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        setSerializationInclusion(JsonInclude.Include.NON_NULL)
    }

    fun convertRawToSimpleHashItem(json: String): SimpleHashItem {
        return mapper.readValue<SimpleHashItem>(json)
    }

    fun convert(source: nft): SimpleHashItem {
        return SimpleHashItem(
            nftId = source.nftId.toString(),
            tokenId = source.tokenId?.toString(),
            name = source.name?.toString(),
            description = source.description?.toString(),
            previews = source.previews?.let { preview ->
                SimpleHashItem.Preview(
                    imageSmallUrl = preview.imageSmallUrl?.toString(),
                    imageMediumUrl = preview.imageMediumUrl?.toString(),
                    imageLargeUrl = preview.imageLargeUrl?.toString(),
                    imageOpengraphUrl = preview.imageOpengraphUrl?.toString()
                )
            },
            imageProperties = source.imageProperties?.let { props ->
                SimpleHashItem.ImageProperties(
                    width = props.width,
                    height = props.height,
                    size = props.size?.let { it.toLong() },
                    mimeType = props.mimeType?.toString()
                )
            },
            createdDate = source.createdDate?.let { LocalDateTime.parse(it) },
            externalUrl = source.externalUrl?.toString(),
            extraMetadata = source.extraMetadata?.let { extractExtraMeta(it.toString()) },

            // is not supported in the kafka event
            collection = null,
        )
    }

    fun extractExtraMeta(source: String): SimpleHashItem.ExtraMetadata {
        return mapper.readValue(StringEscapeUtils.unescapeJson(source))
    }

    fun safeConvertToMetaUpdate(json: String): SimpleHashNftMetadataUpdate? {
        return try {
            mapper.readValue<SimpleHashNftMetadataUpdate>(json)
        } catch (e: Exception) {
            null
        }
    }

    fun convert(itemId: ItemIdDto, item: SimpleHashItem): RawMetaCache {
        val raw = mapper.writeValueAsString(item)
        return RawMetaCache(
            id = cacheId(itemId),
            data = raw,
            createdAt = Instant.now()
        )
    }

    fun cacheId(itemId: ItemIdDto): RawMetaCache.CacheId {
        return RawMetaCache.CacheId(MetaSource.SIMPLE_HASH, itemId.fullId())
    }

    fun parseNftId(nftId: String): ItemIdDto {
        // SimpleHash itemId format is "ethereum.0x8943c7bac1914c9a7aba750bf2b6b09fd21037e0.5903"
        val toUnionFormat = nftId.replace(".", ":").uppercase()
        return IdParser.parseItemId(toUnionFormat)
    }

    fun safeParseTokenId(nftId: String): String? {
        return try {
            val itemIdDto = parseNftId(nftId)
            val parts = IdParser.split(itemIdDto.value, 2)
            return parts[1]
        } catch (ex: Throwable) {
            null
        }
    }
}
