package com.rarible.protocol.union.enrichment.meta.provider

import com.rarible.protocol.union.core.model.UnionMeta
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
    private val simpleHashService: SimpleHashService
) : ItemMetaProvider {

    override suspend fun fetch(key: ItemIdDto): WrappedMeta<UnionMeta>? {
        return if (simpleHashService.isSupported(key.blockchain)) {
            simpleHashService.fetch(key)?.let {
                WrappedMeta(
                    source = MetaSource.SIMPLE_HASH,
                    data = it
                )
            }
        } else null
    }
}
