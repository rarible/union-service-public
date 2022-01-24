package com.rarible.protocol.union.core.model

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.rarible.protocol.union.dto.ItemIdDto

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY)
@JsonSubTypes(
    JsonSubTypes.Type(name = "UPDATE", value = UnionItemUpdateEvent::class),
    JsonSubTypes.Type(name = "DELETE", value = UnionItemDeleteEvent::class)
)
sealed class UnionItemEvent {

    abstract val itemId: ItemIdDto
}

data class UnionItemUpdateEvent(
    override val itemId: ItemIdDto,
    val item: UnionItem
) : UnionItemEvent() {

    constructor(item: UnionItem) : this(item.id, item)

}

data class UnionItemDeleteEvent(
    override val itemId: ItemIdDto
) : UnionItemEvent()