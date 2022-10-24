package com.rarible.protocol.union.enrichment.service

import com.rarible.protocol.union.core.FeatureFlagsProperties
import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.dto.ItemIdDto
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Component

@Primary
@Component
class ItemMetaCompatibilityService(
    legacy: ItemMetaLegacyService,
    modern: ItemMetaModernService,
    ff: FeatureFlagsProperties
) : ItemMetaService {

    private val delegate = if (ff.enableMetaPipeline && !ff.enableMetaLegacyPipeline) modern else legacy

    override suspend fun get(itemIds: List<ItemIdDto>, pipeline: String): Map<ItemIdDto, UnionMeta> {
        return delegate.get(itemIds, pipeline)
    }

    override suspend fun get(itemId: ItemIdDto, sync: Boolean, pipeline: String): UnionMeta? {
        return delegate.get(itemId, sync, pipeline)
    }

    override suspend fun download(itemId: ItemIdDto, pipeline: String, force: Boolean): UnionMeta? {
        return delegate.download(itemId, pipeline, force)
    }

    override suspend fun schedule(itemId: ItemIdDto, pipeline: String, force: Boolean) {
        return delegate.schedule(itemId, pipeline, force)
    }

    override suspend fun save(itemId: ItemIdDto, meta: UnionMeta) {
        return delegate.save(itemId, meta)
    }

    suspend fun onLegacyMetaUpdated() {

    }
}