package com.rarible.protocol.union.core.continuation

import com.rarible.protocol.union.dto.continuation.Continuation
import com.rarible.protocol.union.dto.continuation.ContinuationFactory
import kotlin.math.min

class ContinuationPaging<T, C : Continuation<C>, F : ContinuationFactory<T, C>>(
    private val factory: F,
    private val entities: Collection<T>
) {

    companion object {
        private const val DEFAULT_PAGE_SIZE = 10
    }

    fun getPage(size: Int?): ContinuationPage<T, C> {
        val pageableList = entities.map { PageableEntity(it, factory) }.sorted()

        val preferredPageSize = size ?: DEFAULT_PAGE_SIZE
        val pageSize = min(preferredPageSize, pageableList.size)

        val page = pageableList.subList(0, pageSize)

        val continuation = if (pageableList.size > pageSize) page.last().continuation else null

        return ContinuationPage(page.map { it.entity }, continuation)
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