package com.rarible.protocol.union.integration.ethereum.converter

import com.rarible.protocol.dto.AudioContentDto
import com.rarible.protocol.dto.HtmlContentDto
import com.rarible.protocol.dto.ImageContentDto
import com.rarible.protocol.dto.Model3dContentDto
import com.rarible.protocol.dto.NftCollectionMetaDto
import com.rarible.protocol.dto.NftItemMetaDto
import com.rarible.protocol.dto.NftMediaDto
import com.rarible.protocol.dto.NftMediaMetaDto
import com.rarible.protocol.dto.UnknownContentDto
import com.rarible.protocol.dto.VideoContentDto
import com.rarible.protocol.union.core.model.UnionAudioProperties
import com.rarible.protocol.union.core.model.UnionCollectionMeta
import com.rarible.protocol.union.core.model.UnionHtmlProperties
import com.rarible.protocol.union.core.model.UnionImageProperties
import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.core.model.UnionMetaContent
import com.rarible.protocol.union.core.model.UnionMetaContentProperties
import com.rarible.protocol.union.core.model.UnionModel3dProperties
import com.rarible.protocol.union.core.model.UnionUnknownProperties
import com.rarible.protocol.union.core.model.UnionVideoProperties
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.MetaAttributeDto
import com.rarible.protocol.union.dto.MetaContentDto

object EthMetaConverter {

    fun convert(source: NftItemMetaDto): UnionMeta {
        // Legacy format of Eth meta, should not be used
        val legacyContent = getLegacyContent(source)
        val modernContent = source.content.map { convert(it) }
        val content = modernContent.ifEmpty { legacyContent }
        return UnionMeta(
            name = source.name,
            description = source.description,
            language = source.language,
            genres = source.genres,
            tags = source.tags,
            createdAt = source.createdAt,
            rights = source.rights,
            rightsUri = source.rightsUri,
            externalUri = source.externalUri,
            originalMetaUri = source.originalMetaUri,
            attributes = source.attributes.orEmpty().map {
                MetaAttributeDto(
                    key = it.key,
                    value = it.value,
                    type = it.type,
                    format = it.format
                )
            },
            content = content,
            // TODO deprecated, remove later
            restrictions = emptyList()
        )
    }

    fun convert(source: NftCollectionMetaDto?, blockchain: BlockchainDto): UnionCollectionMeta? {
        if (source == null) return null

        // Legacy format of Eth meta, should not be used
        val legacyContent = convert(source.image)
        val modernContent = source.content.map { convert(it) }
        val content = modernContent.ifEmpty { legacyContent }

        return UnionCollectionMeta(
            name = source.name,
            description = source.description,

            createdAt = source.createdAt,
            tags = source.tags,
            genres = source.genres,
            language = source.language,
            rights = source.rights,
            rightsUri = source.rightsUri,
            externalUri = source.externalUri,
            originalMetaUri = source.originalMetaUri,

            // TODO remove later
            feeRecipient = (source.feeRecipient ?: source.fee_recipient)?.let { EthConverter.convert(it, blockchain) },
            // TODO remove later
            sellerFeeBasisPoints = source.seller_fee_basis_points ?: source.sellerFeeBasisPoints,
            content = content,
            // TODO remove later
            externalLink = source.external_link
        )
    }

    fun convert(source: com.rarible.protocol.dto.MetaContentDto): UnionMetaContent {
        val properties = when (source) {
            is ImageContentDto -> UnionImageProperties(
                mimeType = source.mimeType,
                size = source.size,
                width = source.width,
                height = source.height
            )
            is VideoContentDto -> UnionVideoProperties(
                mimeType = source.mimeType,
                size = source.size,
                width = source.width,
                height = source.height
            )
            is AudioContentDto -> UnionAudioProperties(
                mimeType = source.mimeType,
                size = source.size
            )
            is Model3dContentDto -> UnionModel3dProperties(
                mimeType = source.mimeType,
                size = source.size
            )
            is HtmlContentDto -> UnionHtmlProperties(
                mimeType = source.mimeType,
                size = source.size
            )
            is UnknownContentDto -> UnionUnknownProperties(
                mimeType = source.mimeType,
                size = source.size
            )
        }
        return UnionMetaContent(
            url = source.url,
            fileName = source.fileName,
            representation = MetaContentDto.Representation.valueOf(source.representation.name),
            properties = properties
        )
    }

    @Deprecated("Should be removed")
    fun getLegacyContent(source: NftItemMetaDto): List<UnionMetaContent> {
        return convertMetaContent(source.image) { imageMetaDto ->
            UnionImageProperties(
                mimeType = imageMetaDto?.type,
                width = imageMetaDto?.width,
                height = imageMetaDto?.height,
                size = null // TODO ETHEREUM - get from ETH OpenAPI.
            )
        } + convertMetaContent(source.animation) { videoMetaDto ->
            UnionVideoProperties(
                mimeType = videoMetaDto?.type,
                width = videoMetaDto?.width,
                height = videoMetaDto?.height,
                size = null // TODO ETHEREUM - get from ETH OpenAPI.
            )
        }
    }

    @Deprecated("Should be removed")
    private fun convertMetaContent(
        source: NftMediaDto?,
        converter: (meta: NftMediaMetaDto?) -> UnionMetaContentProperties
    ): List<UnionMetaContent> {
        source ?: return emptyList()
        return source.url.map { (representationType, url) ->
            val meta = source.meta[representationType]
            UnionMetaContent(
                url = url,
                // TODO UNION handle unknown representation
                representation = MetaContentDto.Representation.valueOf(representationType),
                properties = converter(meta)
            )
        }
    }

    @Deprecated("Should be removed")
    private fun convert(sourceImage: NftMediaDto?): List<UnionMetaContent> {
        if (sourceImage == null) return emptyList()
        return sourceImage.url.keys.map { key ->
            convert(key, sourceImage.url[key]!!, sourceImage.meta[key])
        }
    }

    @Deprecated("Should be removed")
    private fun convert(key: String, url: String, meta: NftMediaMetaDto?): UnionMetaContent {
        return UnionMetaContent(
            url = url,
            representation = MetaContentDto.Representation.valueOf(key),
            properties = UnionImageProperties(
                mimeType = meta?.type,
                width = meta?.width,
                height = meta?.height,
                size = null, // TODO find where to get size from
            )
        )
    }

}