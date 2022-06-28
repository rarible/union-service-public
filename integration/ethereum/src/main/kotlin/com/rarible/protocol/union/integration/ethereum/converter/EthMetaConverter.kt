package com.rarible.protocol.union.integration.ethereum.converter

import com.rarible.protocol.dto.AudioContentDto
import com.rarible.protocol.dto.HtmlContentDto
import com.rarible.protocol.dto.ImageContentDto
import com.rarible.protocol.dto.Model3dContentDto
import com.rarible.protocol.dto.NftCollectionMetaDto
import com.rarible.protocol.dto.NftItemMetaDto
import com.rarible.protocol.dto.UnknownContentDto
import com.rarible.protocol.dto.VideoContentDto
import com.rarible.protocol.union.core.model.UnionAudioProperties
import com.rarible.protocol.union.core.model.UnionCollectionMeta
import com.rarible.protocol.union.core.model.UnionHtmlProperties
import com.rarible.protocol.union.core.model.UnionImageProperties
import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.core.model.UnionMetaContent
import com.rarible.protocol.union.core.model.UnionModel3dProperties
import com.rarible.protocol.union.core.model.UnionUnknownProperties
import com.rarible.protocol.union.core.model.UnionVideoProperties
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.MetaAttributeDto
import com.rarible.protocol.union.dto.MetaContentDto

object EthMetaConverter {

    fun convert(source: NftItemMetaDto): UnionMeta {
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
            content = source.content.map { convert(it) },
            // TODO deprecated, remove later
            restrictions = emptyList()
        )
    }

    fun convert(source: NftCollectionMetaDto?, blockchain: BlockchainDto): UnionCollectionMeta? {
        if (source == null) return null

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
            feeRecipient = source.feeRecipient?.let { EthConverter.convert(it, blockchain) },
            sellerFeeBasisPoints = source.sellerFeeBasisPoints,
            content = source.content.map { convert(it) },
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
}
