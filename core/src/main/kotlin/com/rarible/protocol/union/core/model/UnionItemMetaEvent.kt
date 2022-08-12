package com.rarible.protocol.union.core.model

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.rarible.protocol.union.dto.ItemIdDto

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY)
@JsonSubTypes(
    JsonSubTypes.Type(name = "UPDATE", value = UnionItemMetaUpdateEvent::class),
    JsonSubTypes.Type(name = "REFRESH", value = UnionItemMetaRefreshEvent::class)
)
sealed class UnionItemMetaEvent {
    abstract val itemId: ItemIdDto
}

/**
 * Signals the union service that the meta of the item [itemId] has been updated to [unionMeta].
 */
data class UnionItemMetaUpdateEvent(
    override val itemId: ItemIdDto,
    val unionItem: UnionItem?,
    val unionMeta: UnionMeta
) : UnionItemMetaEvent()

/**
 * Signals the union service that a meta refresh is needed for the item [itemId].
 */
data class UnionItemMetaRefreshEvent(
    override val itemId: ItemIdDto
) : UnionItemMetaEvent()