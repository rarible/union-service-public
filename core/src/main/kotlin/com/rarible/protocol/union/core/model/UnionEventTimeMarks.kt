package com.rarible.protocol.union.core.model

import com.rarible.protocol.union.dto.EventTimeMarkDto
import com.rarible.protocol.union.dto.EventTimeMarksDto
import java.time.Instant

data class UnionEventTimeMarks(
    val source: String, val marks: List<UnionSourceEventTimeMark> = emptyList()
) {

    fun add(name: String, date: Instant? = null): UnionEventTimeMarks {
        val mark = UnionSourceEventTimeMark(name, date ?: Instant.now())
        val marks = this.marks.toMutableList()
        marks.add(mark)
        return this.copy(marks = marks)
    }

    fun addOut() = add("enrichment-out")
    fun toDto() = EventTimeMarksDto(source, marks.map { EventTimeMarkDto(it.name, it.date) })

}

data class UnionSourceEventTimeMark(
    val name: String, val date: Instant
)

// Stub event marks for blockchains who don't support time marking
fun stubEventMark(): UnionEventTimeMarks {
    return UnionEventTimeMarks("stub").add("indexer-out")
}

fun offchainEventMark(markName: String, date: Instant? = null): UnionEventTimeMarks {
    return UnionEventTimeMarks("offchain").add(markName, date)
}

fun blockchainEventMark(markName: String, date: Instant): UnionEventTimeMarks {
    return UnionEventTimeMarks("blockchain")
        .add("source", date)
        .add(markName, date)
}

// Workaround for external indexers - we can't get detailed info for it
fun blockchainAndIndexerMarks(date: Instant) = blockchainEventMark("indexer-in", date)
    .add("indexer-out", date)

fun offchainAndIndexerMarks(date: Instant) = offchainEventMark("indexer-in", date)
    .add("indexer-out", date)
