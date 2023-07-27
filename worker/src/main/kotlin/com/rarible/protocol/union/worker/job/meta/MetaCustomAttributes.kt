package com.rarible.protocol.union.worker.job.meta

import com.rarible.protocol.union.core.model.UnionMetaAttribute
import com.rarible.protocol.union.dto.ItemIdDto

data class MetaCustomAttributes(
    val id: ItemIdDto,
    val attributes: List<UnionMetaAttribute>
)
