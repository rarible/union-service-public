package com.rarible.protocol.union.core.event

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

enum class ChangeEventType {
    FAKE,
    OWNERSHIP,
    FOLLOWING,
    ITEM,
    ITEMLIKE,
    TOKEN,
    USER,
    OFFER,
    COLLECTION_OFFER,
    HOT_BID,
    AUCTION,
    WERT,
    ACTIVITY
}

data class ChangeEvent(
    val type: ChangeEventType,
    val value: Any
)

enum class SubscribeRequestType {
    OWNERSHIP,
    FOLLOWING,
    ITEM,
    ITEMLIKE,
    PROFILE,
    OFFER,
    COLLECTION_OFFER,
    AUCTION,
    WERT,
    ACTIVITY
}

/** Действие, осуществляемое с подпиской. */
enum class SubscriptionAction {
    SUBSCRIBE,
    UNSUBSCRIBE
}

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type", visible = true)
@JsonSubTypes(
    JsonSubTypes.Type(name = "ITEM", value = SubscribeRequest::class)
)
sealed class AbstractSubscribeRequest {
    abstract val type: SubscribeRequestType
    abstract val action: SubscriptionAction
}

data class SubscribeRequest(
    override val type: SubscribeRequestType,
    override val action: SubscriptionAction = SubscriptionAction.SUBSCRIBE,
    val id: String
) : AbstractSubscribeRequest()
