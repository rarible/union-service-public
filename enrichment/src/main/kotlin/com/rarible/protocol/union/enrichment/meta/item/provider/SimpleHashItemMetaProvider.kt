package com.rarible.protocol.union.enrichment.meta.item.provider

import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.core.model.UnionMetaContent
import com.rarible.protocol.union.core.model.download.MetaSource
import com.rarible.protocol.union.core.model.download.ProviderDownloadException
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.enrichment.service.SimpleHashService
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty("meta.simpleHash.enabled", havingValue = "true")
class SimpleHashItemMetaProvider(
    private val simpleHashService: SimpleHashService,
) : ItemMetaProvider {

    override fun getSource(): MetaSource = MetaSource.SIMPLE_HASH

    override suspend fun fetch(blockchain: BlockchainDto, id: String, original: UnionMeta?): UnionMeta? {
        if (!simpleHashService.isSupported(blockchain)) {
            return original
        }

        val simpleHashMeta = simpleHashService.fetch(ItemIdDto(blockchain, id))
        if (simpleHashMeta == null || simpleHashMeta.content.isEmpty()) {
            throw ProviderDownloadException(MetaSource.SIMPLE_HASH)
        }

        if (original == null) return simpleHashMeta

        return original.copy(
            contributors = original.contributors + getSource(),
            description = original.description ?: simpleHashMeta.description,
            content = mergeContent(original, simpleHashMeta.content).mergeContentProperties(simpleHashMeta),
            attributes = original.attributes.ifEmpty { simpleHashMeta.attributes },
            createdAt = original.createdAt ?: simpleHashMeta.createdAt,
            externalUri = original.externalUri ?: simpleHashMeta.externalUri,
            originalMetaUri = original.originalMetaUri ?: simpleHashMeta.originalMetaUri
        )
    }

    private fun mergeContent(meta: UnionMeta, simpleHashContent: List<UnionMetaContent>): List<UnionMetaContent> {
        val existed = meta.content.map { it.representation }.toSet()
        val adding = simpleHashContent.filterNot { it.representation in existed }
        return meta.content + adding
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
