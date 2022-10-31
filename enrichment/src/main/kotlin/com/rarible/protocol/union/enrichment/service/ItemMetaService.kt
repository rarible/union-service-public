package com.rarible.protocol.union.enrichment.service

import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.enrichment.meta.item.ItemMetaPipeline

interface ItemMetaService {

    suspend fun get(itemIds: List<ItemIdDto>, pipeline: ItemMetaPipeline): Map<ItemIdDto, UnionMeta>

    suspend fun get(itemId: ItemIdDto, sync: Boolean, pipeline: ItemMetaPipeline): UnionMeta?

    suspend fun download(itemId: ItemIdDto, pipeline: ItemMetaPipeline, force: Boolean): UnionMeta?

    suspend fun schedule(itemId: ItemIdDto, pipeline: ItemMetaPipeline, force: Boolean)

    suspend fun save(itemId: ItemIdDto, meta: UnionMeta)
}