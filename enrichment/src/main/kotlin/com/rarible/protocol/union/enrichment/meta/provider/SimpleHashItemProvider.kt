package com.rarible.protocol.union.enrichment.meta.provider

import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.core.model.UnionMetaAttribute
import com.rarible.protocol.union.core.model.UnionMetaContent
import com.rarible.protocol.union.core.model.download.MetaProviderType
import com.rarible.protocol.union.core.model.download.ProviderDownloadException
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.enrichment.meta.MetaSource
import com.rarible.protocol.union.enrichment.meta.WrappedMeta
import com.rarible.protocol.union.enrichment.meta.item.ItemMetaProvider
import com.rarible.protocol.union.enrichment.service.SimpleHashService
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty("meta.simpleHash.enabled", havingValue = "true")
class SimpleHashItemProvider(
    private val simpleHashService: SimpleHashService,
) : ItemMetaProvider {

    override fun getType(): MetaProviderType = MetaProviderType.SIMPLE_HASH

    override suspend fun fetch(key: ItemIdDto, original: WrappedMeta<UnionMeta>?): WrappedMeta<UnionMeta>? {
        return if (simpleHashService.isSupported(key.blockchain)) {
            simpleHashService.fetch(key)?.let { simpleHashMeta ->
                return WrappedMeta(
                    source = original?.source ?: MetaSource.SIMPLE_HASH,
                    data = original?.data?.copy(
                        description = original.data.description ?: simpleHashMeta.description,
                        content = mergeContent(original.data, simpleHashMeta.content)
                            .mergeContentProperties(simpleHashMeta),
                        attributes = mergeAttrs(original.data, simpleHashMeta.attributes),
                        createdAt = original.data.createdAt ?: simpleHashMeta.createdAt,
                        externalUri = original.data.externalUri ?: simpleHashMeta.externalUri,
                        originalMetaUri = original.data.originalMetaUri ?: simpleHashMeta.originalMetaUri
                    ) ?: simpleHashMeta
                )
            } ?: throw ProviderDownloadException(MetaProviderType.SIMPLE_HASH)
        } else {
            original
        }
    }

    private fun mergeContent(meta: UnionMeta, simpleHashContent: List<UnionMetaContent>): List<UnionMetaContent> {
        val existed = meta.content.map { it.representation }.toSet()
        val adding = simpleHashContent.filterNot { it.representation in existed }
        return meta.content + adding
    }

    private fun mergeAttrs(meta: UnionMeta, attrs: List<UnionMetaAttribute>): List<UnionMetaAttribute> {
        val existed = meta.attributes.map { it.key }.toSet()
        val adding = attrs.filterNot { it.key in existed }
        return meta.attributes + adding
    }

    private fun List<UnionMetaContent>.mergeContentProperties(simpleHashMeta: UnionMeta): List<UnionMetaContent> {
        return map {
            val properties = simpleHashMeta.content
                .firstOrNull { fromSimpleHash -> fromSimpleHash.url == it.url }
                ?.properties

            if (properties != null) {
                val merged = it.properties?.merge(properties) ?: properties
                it.copy(properties = merged)
            } else {
                it
            }
        }
    }
}
