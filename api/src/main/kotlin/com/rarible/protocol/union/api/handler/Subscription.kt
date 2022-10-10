package com.rarible.protocol.union.api.handler

/**
 * Represents subscription to specific set of events
 * Using this object we can find out if there is an active subscription for the event (call covers function)
 */
abstract class Subscription<T> {
    /**
     * Should return true if subscription covers event.
     * For example, OrderSubscription with null types and plaforms covers all events,
     * but if platform is defined and is not equal to event's platform, then function returns false
     */
    abstract fun covers(event: T): Boolean

    companion object {
        /**
         * Tells if subscribed to event
         */
        fun <T> isSubscribed(
            subscriptions: Map<Any, Set<Subscription<T>>>,
            event: T,
            vararg ids: Any?
        ): Boolean {
            val subs = ids.filterNotNull().map { subscriptions[it] ?: emptySet() }.flatten().toSet()
            return subs.any { it.covers(event) }
        }

        fun <T> handleSubscriptionRequest(
            subscribe: Boolean,
            id: Any,
            subscriptions: MutableMap<Any, Set<Subscription<T>>>,
            subscription: Subscription<T>,
        ) {
            if (subscribe) {
                subscriptions.compute(id) { _, set ->
                    if (set == null) {
                        setOf(subscription)
                    } else {
                        set + subscription
                    }
                }
            } else {
                subscriptions.compute(id) { _, set ->
                    if (set == null) {
                        emptySet()
                    } else {
                        set - subscription
                    }
                }
            }
        }
    }
}