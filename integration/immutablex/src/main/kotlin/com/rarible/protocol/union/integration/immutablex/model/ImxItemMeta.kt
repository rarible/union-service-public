package com.rarible.protocol.union.integration.immutablex.model

import com.rarible.protocol.union.core.model.UnionMeta
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.FieldType
import org.springframework.data.mongodb.core.mapping.MongoId

@Document("immutablex_item_meta")
data class ImxItemMeta(
    @MongoId(FieldType.STRING)
    val id: String,
    val meta: UnionMeta
)
