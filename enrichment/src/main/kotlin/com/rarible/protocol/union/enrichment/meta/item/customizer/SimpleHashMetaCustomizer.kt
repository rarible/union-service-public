package com.rarible.protocol.union.enrichment.meta.item.customizer

import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.core.model.UnionMetaAttribute
import com.rarible.protocol.union.core.model.UnionMetaContent
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.enrichment.meta.MetaSource
import com.rarible.protocol.union.enrichment.meta.content.ContentMetaDownloader
import com.rarible.protocol.union.enrichment.meta.item.ItemMetaCustomizer
import com.rarible.protocol.union.enrichment.meta.WrappedMeta
import com.rarible.protocol.union.enrichment.service.SimpleHashService
import com.rarible.protocol.union.enrichment.util.sanitizeContent
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty("meta.simpleHash.enabled", havingValue = "true")
class SimpleHashMetaCustomizer(
    val contentMetaLoader: ContentMetaDownloader,
    val simpleHashService: SimpleHashService
) : ItemMetaCustomizer {

    override suspend fun customize(id: ItemIdDto, meta: WrappedMeta<UnionMeta>): WrappedMeta<UnionMeta> {
        if (meta.source == MetaSource.SIMPLE_HASH) return meta
        val simpleHashMeta = simpleHashService.fetch(id) ?: return meta

        val sanitized = sanitizeContent(simpleHashMeta.content)
        val content = contentMetaLoader.enrichContent(id.value, id.blockchain, sanitized)

        return WrappedMeta(
            source = meta.source,
            data = meta.data.copy(
                description = meta.data.description ?: simpleHashMeta.description,
                content = mergeContent(meta.data, content),
                attributes = mergeAttrs(meta.data, simpleHashMeta.attributes),
                createdAt = meta.data.createdAt ?: simpleHashMeta.createdAt,
                externalUri = meta.data.externalUri ?: simpleHashMeta.externalUri,
                originalMetaUri = meta.data.originalMetaUri ?: simpleHashMeta.originalMetaUri
            )
        )
    }

    private fun mergeContent(meta: UnionMeta, content: List<UnionMetaContent>): List<UnionMetaContent> {
        val existed = meta.content.map { it.representation }.toSet()
        val adding = content.filterNot { it.representation in existed }
        return meta.content + adding
    }

    private fun mergeAttrs(meta: UnionMeta, attrs: List<UnionMetaAttribute>): List<UnionMetaAttribute> {
        val existed = meta.attributes.map { it.key }.toSet()
        val adding = attrs.filterNot { it.key in existed }
        return meta.attributes + adding
    }
}
