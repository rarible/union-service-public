package com.rarible.protocol.union.enrichment.meta.embedded

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document("meta_content_embedded")
class UnionEmbeddedContent(
    @Id
    val id: String,
    val mimeType: String,
    val size: Int,
    val data: ByteArray,
    val available: Boolean?
)