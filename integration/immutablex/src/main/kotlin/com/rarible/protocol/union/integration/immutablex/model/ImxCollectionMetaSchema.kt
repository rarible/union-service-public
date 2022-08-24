package com.rarible.protocol.union.integration.immutablex.model

import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.FieldType
import org.springframework.data.mongodb.core.mapping.MongoId

@Document("immutablex_collection_meta_schema")
class ImxCollectionMetaSchema(
    @MongoId(FieldType.STRING)
    val collection: String,
    val traits: List<ImxTrait> = emptyList()
)

data class ImxTrait(
    val key: String,
    val type: String
)