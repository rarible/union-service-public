package com.rarible.protocol.union.enrichment.model

import com.rarible.protocol.union.core.model.UnionMetaAttribute
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document("meta-custom-attributes")
data class ItemMetaCustomAttributes(
    @Id
    val id: String,
    val attributes: List<UnionMetaAttribute>
)
