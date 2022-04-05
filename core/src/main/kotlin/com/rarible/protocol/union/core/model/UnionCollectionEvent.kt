package com.rarible.protocol.union.core.model

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.rarible.protocol.union.dto.CollectionDto
import com.rarible.protocol.union.dto.CollectionIdDto

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY)
@JsonSubTypes(
    JsonSubTypes.Type(name = "UPDATE", value = UnionItemUpdateEvent::class)
)

sealed  class UnionCollectionEvent {
    abstract val collectionId: CollectionIdDto
}

data class UnionCollectionUpdateEvent(
    override val collectionId: CollectionIdDto,
    val collection: CollectionDto
) : UnionCollectionEvent() {
    constructor(collection: CollectionDto) : this(collection.id, collection)
}

