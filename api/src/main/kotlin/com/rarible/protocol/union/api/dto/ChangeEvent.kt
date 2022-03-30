package com.rarible.protocol.union.api.dto

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

enum class ChangeEventType {
    OWNERSHIP,
    ITEM,
    FAKE
}

data class ChangeEvent(
    val type: ChangeEventType,
    val value: Any
)

enum class SubscribeRequestType {
    OWNERSHIP,
    ITEM
}

/** Действие, осуществляемое с подпиской. */
enum class SubscriptionAction {
    SUBSCRIBE,
    UNSUBSCRIBE
}

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type", visible = true)
@JsonSubTypes(
    JsonSubTypes.Type(name = "OWNERSHIP", value = SubscribeRequest::class),
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

