package com.rarible.protocol.union.api.handler

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class SubscriptionTest {

    data class TestSubscription(
        val value: String
    ): Subscription<String>() {
        override fun covers(event: String): Boolean {
            return event.startsWith(value)
        }
    }

    @Test
    fun `isSubscribed when subscription is in the last element`() {
        val map = mapOf<Any, Set<Subscription<String>>>(
            1 to setOf(TestSubscription("test1")),
            2 to setOf(TestSubscription("test2")),
            3 to setOf(TestSubscription("test3"))
        )
        assertThat(Subscription.isSubscribed(map, "test333", 1, 2, 3))
            .isTrue()
    }

    @Test
    fun `isSubscribed when no subscription found`() {
        val map = mapOf<Any, Set<Subscription<String>>>(
            1 to setOf(TestSubscription("test1")),
            2 to setOf(TestSubscription("test2")),
            3 to setOf(TestSubscription("test3"))
        )
        assertThat(Subscription.isSubscribed(map, "test555", 1, 2, 3))
            .isFalse()
    }
}