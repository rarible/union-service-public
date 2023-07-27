package com.rarible.protocol.union.enrichment.meta.item

import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.enrichment.meta.MetaCustomizer

interface ItemMetaCustomizer : MetaCustomizer<ItemIdDto, UnionMeta>
