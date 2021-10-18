package com.rarible.protocol.union.dto

class UnionEventTopicProvider {

    companion object {
        const val VERSION = "v1"

        fun getCollectionTopic(environment: String): String {
            return "protocol.$environment.union.collection"
        }

        fun getItemTopic(environment: String): String {
            return "protocol.$environment.union.item"
        }

        fun getOwnershipTopic(environment: String): String {
            return "protocol.$environment.union.ownership"
        }

        fun getOrderTopic(environment: String): String {
            return "protocol.$environment.union.order"
        }

        fun getActivityTopic(environment: String): String {
            return "protocol.$environment.union.activity"
        }
    }
}
