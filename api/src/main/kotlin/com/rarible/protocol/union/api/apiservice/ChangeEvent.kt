package com.rarible.apiservice

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.rarible.domain.ActivityFilter
import com.rarible.domain.ActivityType

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
    JsonSubTypes.Type(name = "OWNERSHIP", value = SubscribeRequest::class),
    JsonSubTypes.Type(name = "FOLLOWING", value = SubscribeRequest::class),
    JsonSubTypes.Type(name = "ITEM", value = SubscribeRequest::class),
    JsonSubTypes.Type(name = "ITEMLIKE", value = SubscribeRequest::class),
    JsonSubTypes.Type(name = "PROFILE", value = SubscribeRequest::class),
    JsonSubTypes.Type(name = "OFFER", value = SubscribeRequest::class),
    JsonSubTypes.Type(name = "COLLECTION_OFFER", value = SubscribeRequest::class),
    JsonSubTypes.Type(name = "AUCTION", value = SubscribeRequest::class),
    JsonSubTypes.Type(name = "WERT", value = SubscribeRequest::class),
    JsonSubTypes.Type(name = "ACTIVITY", value = ActivitySubscribeRequest::class)
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

data class ActivitySubscribeRequest(
    override val type: SubscribeRequestType,
    override val action: SubscriptionAction = SubscriptionAction.SUBSCRIBE,
    val activityFilter: ActivityFilter? = null,
    val activityTypes: List<ActivityType> = emptyList()
) : AbstractSubscribeRequest()
