package com.rarible.protocol.union.core.model

import com.rarible.protocol.union.dto.ItemIdDto

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