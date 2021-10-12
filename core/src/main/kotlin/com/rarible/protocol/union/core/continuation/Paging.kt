package com.rarible.protocol.union.core.continuation

import com.rarible.protocol.union.dto.continuation.Continuation
import com.rarible.protocol.union.dto.continuation.ContinuationFactory
import kotlin.math.min

class Paging<T, C : Continuation<C>, F : ContinuationFactory<T, C>>(
    private val factory: F,
    private val entities: Collection<T>
) {

    fun getPage(size: Int, total: Long): Page<T> {
        val slice = getSlice(size)
        return Page(
            total = total,
            continuation = slice.continuation,
            entities = slice.entities
        )
    }

    fun getSlice(size: Int): Slice<T> {
        val pageableList = entities.map { PageableEntity(it, factory) }.sorted()
        val pageSize = min(size, pageableList.size)
        val page = pageableList.subList(0, pageSize)

        val continuation = if (page.size >= size) page.last().continuation else null

        return Slice(
            continuation = continuation?.toString(),
            entities = page.map { it.entity }
        )
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
}