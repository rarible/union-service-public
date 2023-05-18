package com.rarible.protocol.union.enrichment.meta.item

import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.enrichment.meta.provider.MetaProvider

interface ItemMetaProvider : MetaProvider<ItemIdDto, UnionMeta>
