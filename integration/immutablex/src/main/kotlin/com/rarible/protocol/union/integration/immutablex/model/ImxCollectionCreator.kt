package com.rarible.protocol.union.integration.immutablex.model

import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.FieldType
import org.springframework.data.mongodb.core.mapping.MongoId

@Document("immutablex_collection_creator")
data class ImxCollectionCreator(
    @MongoId(FieldType.STRING)
    val collection: String,
    val creator: String
)
