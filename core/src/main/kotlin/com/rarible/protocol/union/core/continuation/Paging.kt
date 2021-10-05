package com.rarible.protocol.union.core.continuation

import com.rarible.protocol.union.dto.continuation.Continuation
import com.rarible.protocol.union.dto.continuation.ContinuationFactory
import kotlin.math.min

class Paging<T, C : Continuation<C>, F : ContinuationFactory<T, C>>(
    private val factory: F,
    private val entities: Collection<T>
) {

    fun getPage(size: Int, total: Long): Page<T> {
        return createSlice(size).asPage(total)
    }

    fun getSlice(size: Int): Slice<T> {
        return createSlice(size).asSlice()
    }

    private fun createSlice(size: Int): ContinuationSlice<T, C> {
        val pageableList = entities.map { PageableEntity(it, factory) }.sorted()
        val pageSize = min(size, pageableList.size)
        val page = pageableList.subList(0, pageSize)

        val continuation = if (page.size >= size) page.last().continuation else null

        return ContinuationSlice(page.map { it.entity }, continuation)
    }

    private data class PageableEntity<T, C : Continuation<C>, F : ContinuationFactory<T, C>>(
        val entity: T,
        private val factory: F
    ) : Comparable<PageableEntity<T, C, F>> {

        val continuation = factory.getContinuation(entity)

        override fun compareTo(other: PageableEntity<T, C, F>): Int {
            return this.continuation.compareTo(other.continuation)
        }
    }

    private data class ContinuationSlice<T, C>(
        val entities: List<T>,
        val continuation: C?
    ) {

        fun asSlice(): Slice<T> {
            return Slice(
                continuation = continuation?.toString(),
                entities = entities
            )
        }

        fun asPage(total: Long): Page<T> {
            return Page(
                continuation = continuation?.toString(),
                entities = entities,
                total = total
            )
        }
    }

}