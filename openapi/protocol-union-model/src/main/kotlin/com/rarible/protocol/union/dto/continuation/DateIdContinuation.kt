package com.rarible.protocol.union.dto.continuation

import java.time.Instant

data class DateIdContinuation(
    val date: Instant,
    val id: String
) : Continuation<DateIdContinuation> {

    override fun toString(): String {
        return "${date.toEpochMilli()}_${id}"
    }

    override fun compareTo(other: DateIdContinuation): Int {
        val dateDiff = this.date.compareTo(other.date)
        if (dateDiff != 0) {
            return -dateDiff
        }
        return -this.id.compareTo(other.id)
    }

    companion object {
        fun parse(str: String?): DateIdContinuation? {
            if (str == null || str.isEmpty()) {
                return null
            }
            val index = str.indexOf('_')
            if (index < 0) {
                return null
            }
            val timestamp = str.substring(0, index)
            val id = str.substring(index + 1)
            return DateIdContinuation(Instant.ofEpochMilli(timestamp.toLong()), id)
        }
    }


}
