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

fun offchainEventMark(markName: String): UnionEventTimeMarks {
    return UnionEventTimeMarks("offchain").add(markName)
}

fun blockchainEventMark(markName: String, date: Instant): UnionEventTimeMarks {
    return UnionEventTimeMarks("blockchain")
        .add("source", date)
        .add(markName, date)
}

fun blockchainAndIndexerOutMarks(date: Instant) = blockchainEventMark("indexer-out", date)
