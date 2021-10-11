package com.rarible.protocol.union.core.continuation

import com.rarible.protocol.union.dto.continuation.Continuation
import com.rarible.protocol.union.dto.continuation.ContinuationFactory

/**
 * Paging implementation for per-arg based requests. Used for building ordered list
 * of Orders which should be requested by several currencies, but ordered by USD.
 */
class ArgPaging<T, C : Continuation<C>, F : ContinuationFactory<T, C>>(
    // Intermediate factory for continuations (to build per-arg continuations)
    private val intermediateFactory: F,
    // Final factory to build final slice (originally, we don't need it to build continuation,
    // it needed just for sort - good example of bad architecture)
    private val finalFactory: F,
    private val slices: List<ArgSlice<T>>
) {

    fun getSlice(size: Int): Slice<T> {
        val continuation = HashMap<String, String>()

        // Let's check finished slices first and mark them completed in final continuation
        val finishedArgs = slices.filter { it.isFinished() }
        finishedArgs.forEach { continuation[it.arg] = ArgSlice.COMPLETED }

        // Then we're building final slice based on non-empty slices
        val inProgressArgs = slices.filter { !it.isFinished() }
        val allEntities = inProgressArgs.flatMap { it.slice.entities }
        val paging = Paging(finalFactory, allEntities)
        val result = paging.getSlice(size)

        // Now we need to understand, which args fully handled and which are still have continuation -
        // we need to check, which entities have got to our result slice
        val resultSet = result.entities.toSet()

        inProgressArgs.forEach {
            // we will check in reversed order because we interested only in last entities
            // per arg that we have in result slice
            val reversed = it.slice.entities.reversed()

            for (entity in reversed) {
                // Ok, we found one of entities for this arg in result slice - we need to build arg continuation for it
                if (resultSet.contains(entity)) {
                    if (it.hasNext() || entity != it.slice.entities.last()) {
                        // If this slice has next page OR this entity is not last from this arg slice,
                        // build continuation for it
                        continuation[it.arg] = intermediateFactory.getContinuation(entity).toString()
                    } else {
                        // Otherwise, we handled all entities for this arg and we don't want to request it anymore
                        continuation[it.arg] = ArgSlice.COMPLETED
                    }
                    // Breaking search since we found the latest match of entity for this arg slice
                    break
                }
            }
            // Case when no one of arg slice entities found in result slice - it means, we need to return same
            // continuation we received in current request
            if (!continuation.contains(it.arg) && it.argContinuation != null) {
                // Continuation for the arg may be null (at first request) - in such case it should still be null,
                // because we can't make continuation for arg which was not started to be handled
                continuation[it.arg] = it.argContinuation
            }
        }

        // Now we need to understand - do we have handled all args or there are still remain some
        // If we don't have continuation for ALL args, it means there are still remain args never requested
        if (continuation.size == slices.size) {
            // Here we're checking ALL of our continuations have constant value COMPLETED
            val uniqueArgContinuations = continuation.values.toSet()
            if (uniqueArgContinuations.size == 1 && uniqueArgContinuations.contains(ArgSlice.COMPLETED)) {
                // If so, we're able to return null continuation here
                return result.copy(continuation = null)
            }
        }

        return result.copy(continuation = CombinedContinuation(continuation).toString())
    }

}