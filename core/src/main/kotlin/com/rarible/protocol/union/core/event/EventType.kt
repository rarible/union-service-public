package com.rarible.protocol.union.core.event

enum class EventType(
    val value: String
) {

    ORDER("order"),
    AUCTION("auction"),
    ACTIVITY("activity"),
    ITEM("item"),
    ITEM_META("itemMeta"),
    COLLECTION("collection"),
    OWNERSHIP("ownership")
}
