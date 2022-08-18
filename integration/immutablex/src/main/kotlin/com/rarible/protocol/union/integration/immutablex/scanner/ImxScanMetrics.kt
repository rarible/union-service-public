package com.rarible.protocol.union.integration.immutablex.scanner

import com.rarible.core.common.nowMillis
import com.rarible.protocol.union.integration.immutablex.model.ImxScanState
import io.micrometer.core.instrument.ImmutableTag
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

class ImxScanMetrics(
    private val meterRegistry: MeterRegistry
) {

    private val scanGauges = ConcurrentHashMap<String, AtomicLong>()

    fun onStateUpdated(state: ImxScanState) {
        if (state.entityDate == null) {
            return
        }
        val lag = (nowMillis().toEpochMilli() - state.entityDate.toEpochMilli()) / 1000
        scanGauges.getOrPut(state.id) {
            meterRegistry.gauge(
                IMX_STATE,
                listOf(tag("type", state.id)),
                AtomicLong(0)
            )
        }.set(lag)
    }

    fun onScanError(state: ImxScanState, errorReason: String) {
        increment(IMX_SCAN_ERROR, tag("type", state.id), tag("reason", errorReason))
    }

    fun onEventError(entityType: String) {
        increment(IMX_EVENT_ERROR, tag("type", entityType))
    }

    private fun increment(name: String, vararg tags: Tag) {
        return meterRegistry.counter(name, tags.toList()).increment()
    }

    private fun tag(key: String, value: String): Tag {
        return ImmutableTag(key, value)
    }

    private companion object {

        const val IMX_STATE = "immutablex_scan_lag"
        const val IMX_SCAN_ERROR = "immutablex_scan_error"
        const val IMX_EVENT_ERROR = "immutablex_event_error"
    }

}