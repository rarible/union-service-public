package com.rarible.protocol.union.listener.handler

import com.rarible.protocol.union.dto.UnionEventTopicProvider

val ITEM_EVENT_HEADERS = mapOf("protocol.union.item.event.version" to UnionEventTopicProvider.VERSION)
val OWNERSHIP_EVENT_HEADERS = mapOf("protocol.union.ownership.event.version" to UnionEventTopicProvider.VERSION)
val ORDER_EVENT_HEADERS = mapOf("protocol.union.order.event.version" to UnionEventTopicProvider.VERSION)
