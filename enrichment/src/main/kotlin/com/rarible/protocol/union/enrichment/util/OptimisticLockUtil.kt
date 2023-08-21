package com.rarible.protocol.union.enrichment.util

import org.springframework.dao.DuplicateKeyException
import org.springframework.dao.OptimisticLockingFailureException
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

suspend fun <T, E> optimisticLockWithInitial(initial: T?, attempts: Long = 5, update: suspend (initial: T?) -> E): E {
    val current = AtomicReference(initial)
    val retry = AtomicInteger(0)
    val last = AtomicReference<Throwable?>(null)

    do {
        val exception = try {
            return update(current.get())
        } catch (ex: OptimisticLockingFailureException) {
            ex
        } catch (ex: DuplicateKeyException) {
            ex
        }
        last.set(exception)
        current.set(null)
    } while (retry.incrementAndGet() < attempts)

    throw last.get()!!
}
