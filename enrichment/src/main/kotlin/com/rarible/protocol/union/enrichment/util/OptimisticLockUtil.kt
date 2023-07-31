package com.rarible.protocol.union.enrichment.util

import org.springframework.dao.DuplicateKeyException
import org.springframework.dao.OptimisticLockingFailureException

suspend fun <T, E> optimisticLockWithInitial(initial: T?, attempts: Long = 5, update: suspend (initial: T?) -> E): E {
    var current = initial
    var retry = 0
    var last: Throwable

    do {
        last = try {
            return update(current)
        } catch (ex: OptimisticLockingFailureException) {
            ex
        } catch (ex: DuplicateKeyException) {
            ex
        }
        current = null
    } while (++retry < attempts)

    throw last
}
