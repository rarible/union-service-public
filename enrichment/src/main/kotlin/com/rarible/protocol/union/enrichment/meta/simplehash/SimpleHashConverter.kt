package com.rarible.protocol.union.enrichment.meta.simplehash

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.rarible.protocol.union.core.model.UnionImageProperties
import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.core.model.UnionMetaAttribute
import com.rarible.protocol.union.core.model.UnionMetaContent
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.MetaContentDto
import com.rarible.protocol.union.dto.parser.IdParser
import com.rarible.protocol.union.enrichment.meta.MetaSource
import com.rarible.protocol.union.enrichment.model.RawMetaCache
import java.time.Instant
import java.time.ZoneOffset

object SimpleHashConverter {
    private val mapper = ObjectMapper().run {
        registerKotlinModule()
        registerModule(JavaTimeModule())
        configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        setSerializationInclusion(JsonInclude.Include.NON_NULL)
    }

    fun convert(source: SimpleHashItem): UnionMeta {
        return UnionMeta(
            name = source.name ?: "${source.collection?.name} #${safeParseTokenId(source.nftId)}",
            collectionId = null,
            description = source.description ?: source.collection?.description,
            createdAt = source.createdDate?.toInstant(ZoneOffset.UTC),
            tags = emptyList(),
            genres = emptyList(),
            language = null,
            rights = null,
            rightsUri = null,
            externalUri = source.metadataOriginalUrl,
            originalMetaUri = source.metadataOriginalUrl,
            attributes = attributes(source),
            content = content(source),
            restrictions = emptyList()
        )
    }

    fun convertRawToSimpleHashItem(json: String): SimpleHashItem {
        return mapper.readValue<SimpleHashItem>(json)
    }

    fun convertRawToUnionMeta(json: String): UnionMeta {
        return convert(convertRawToSimpleHashItem(json))
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
        val toUnionFormat = nftId.replace(".",":").uppercase()
        return IdParser.parseItemId(toUnionFormat)
    }

    private fun safeParseTokenId(nftId: String): String? {
        return try {
            val itemIdDto = parseNftId(nftId)
            val parts = IdParser.split(itemIdDto.value, 2)
            return parts[1]
        } catch (ex: Throwable) {
            null
        }
    }

    private fun content(source: SimpleHashItem): List<UnionMetaContent> {
        return listOfNotNull(
            source.previews?.imageLargeUrl?.let {
                UnionMetaContent(
                    url = it,
                    representation = MetaContentDto.Representation.BIG
                )
            },
            source.previews?.imageOpengraphUrl?.let {
                UnionMetaContent(
                    url = it,
                    representation = MetaContentDto.Representation.PORTRAIT
                )
            },
            source.previews?.imageSmallUrl?.let {
                UnionMetaContent(
                    url = it,
                    representation = MetaContentDto.Representation.PREVIEW
                )
            },
            source.extraMetadata?.imageOriginalUrl?.let {
                UnionMetaContent(
                    url = it,
                    representation = MetaContentDto.Representation.ORIGINAL,
                    // Supposed that SimpleHash item meta property "image_properties" applies to "extra_metadata.image_original_url"
                    properties = source.imageProperties?.let { properties ->
                        UnionImageProperties(
                            size = properties.size,
                            width = properties.width,
                            height = properties.height,
                            mimeType = properties.mimeType
                        )
                    }
                )
            }
        )
    }

    private fun attributes(source: SimpleHashItem): List<UnionMetaAttribute> {
        return source.extraMetadata?.attributes?.map {
            UnionMetaAttribute(key = it.traitType, value = it.value)
        } ?: emptyList()
    }

}
