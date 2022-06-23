package com.rarible.protocol.union.enrichment.service

import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.dto.ItemIdDto

interface ItemMetaService {

    suspend fun get(itemIds: List<ItemIdDto>, pipeline: String): Map<ItemIdDto, UnionMeta>

    suspend fun get(itemId: ItemIdDto, sync: Boolean, pipeline: String): UnionMeta?

    suspend fun download(itemId: ItemIdDto, pipeline: String, force: Boolean): UnionMeta?

    suspend fun schedule(itemId: ItemIdDto, pipeline: String, force: Boolean)

    suspend fun save(itemId: ItemIdDto, meta: UnionMeta)
}