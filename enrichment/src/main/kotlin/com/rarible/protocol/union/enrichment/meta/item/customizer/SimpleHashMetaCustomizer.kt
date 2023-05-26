package com.rarible.protocol.union.enrichment.meta.item.customizer

import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.core.model.UnionMetaAttribute
import com.rarible.protocol.union.core.model.UnionMetaContent
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.enrichment.meta.MetaSource
import com.rarible.protocol.union.enrichment.meta.WrappedMeta
import com.rarible.protocol.union.enrichment.meta.content.ContentMetaDownloader
import com.rarible.protocol.union.enrichment.meta.item.ItemMetaCustomizer
import com.rarible.protocol.union.enrichment.service.SimpleHashService
import com.rarible.protocol.union.enrichment.util.sanitizeContent
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

// Should be the first in customizer list since data received here might also be modified by other customizers
// TODO ideally, refactor it not as customizer, but maybe contributor/extender which are called BEFORE customizers
@Order(0)
@Component
@ConditionalOnProperty("meta.simpleHash.enabled", havingValue = "true")
class SimpleHashMetaCustomizer(
    val contentMetaLoader: ContentMetaDownloader,
    val simpleHashService: SimpleHashService
) : ItemMetaCustomizer {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun customize(id: ItemIdDto, wrappedMeta: WrappedMeta<UnionMeta>): WrappedMeta<UnionMeta> {
        if (wrappedMeta.source == MetaSource.SIMPLE_HASH) {
            logger.info("Meta was fetched from simplehash, skipping customizer")
            return wrappedMeta
        }
        val simpleHashMeta = simpleHashService.fetch(id) ?: return wrappedMeta

        logger.info("Customizing meta for Item {} with {}", id.fullId(), javaClass.simpleName)

        val sanitized = sanitizeContent(simpleHashMeta.content)
        val content = contentMetaLoader.enrichContent(id.value, id.blockchain, sanitized)

        return WrappedMeta(
            source = wrappedMeta.source,
            data = wrappedMeta.data.copy(
                description = wrappedMeta.data.description ?: simpleHashMeta.description,
                content = mergeContent(wrappedMeta.data, content),
                attributes = mergeAttrs(wrappedMeta.data, simpleHashMeta.attributes),
                createdAt = wrappedMeta.data.createdAt ?: simpleHashMeta.createdAt,
                externalUri = wrappedMeta.data.externalUri ?: simpleHashMeta.externalUri,
                originalMetaUri = wrappedMeta.data.originalMetaUri ?: simpleHashMeta.originalMetaUri
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
