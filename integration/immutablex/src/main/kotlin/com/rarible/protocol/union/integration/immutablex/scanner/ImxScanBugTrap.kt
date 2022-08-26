package com.rarible.protocol.union.integration.immutablex.scanner

import com.rarible.core.common.nowMillis
import com.rarible.protocol.union.integration.immutablex.client.ImmutablexEvent
import org.slf4j.LoggerFactory
import java.time.Instant

class ImxScanBugTrap(
    private val type: ImxScanEntityType
) {

    companion object {

        private val logger = LoggerFactory.getLogger(ImxScanBugTrap::class.java)
    }

    @Volatile
    private var prevTail: List<Long> = emptyList()

    @Volatile
    private var prevFromTx: Long = 0

    @Volatile
    private var prevToDate: Instant = nowMillis()

    fun onNext(fromTx: Long, received: List<ImmutablexEvent>, toDate: Instant) {

        val prev = prevTail.iterator()
        val current = received.iterator()
        while (prev.hasNext() && current.hasNext()) {
            val p = prev.next()
            val c = current.next()
            if (p != c.transactionId) {
                val start = received.map { it.transactionId }
                logger.warn(
                    "Received {$type} Immutablex page with unexpected event order/content," +
                        "expected start: $prevTail, but received page: $start; " +
                        "previous request: transaction_id=$prevFromTx, trimmed to $prevToDate; " +
                        "current request: transaction_id=$fromTx, trimmed to $toDate"
                )
                break
            }
        }

        prevFromTx = fromTx
        prevToDate = toDate
        prevTail = received.filter { it.timestamp.isAfter(toDate) || it.timestamp == toDate }
            .map { it.transactionId }
    }
}