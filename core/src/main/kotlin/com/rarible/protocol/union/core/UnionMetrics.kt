package com.rarible.protocol.union.core

import com.rarible.protocol.union.dto.BlockchainDto
import io.micrometer.core.instrument.ImmutableTag
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag

abstract class UnionMetrics(
    protected val meterRegistry: MeterRegistry
) {

    protected fun tag(blockchain: BlockchainDto): Tag {
        return tag("blockchain", blockchain.name.lowercase())
    }

    protected fun tag(key: String, value: String): Tag {
        return ImmutableTag(key, value)
    }

    protected fun status(status: String): Tag {
        return tag("status", status)
    }

    protected fun type(type: String): Tag {
        return tag("type", type)
    }

    protected fun reason(reason: String): Tag {
        return tag("reason", reason)
    }

    protected fun source(value: String): Tag {
        return tag("source", value)
    }

    protected fun increment(name: String, vararg tags: Tag) {
        return meterRegistry.counter(name, tags.toList()).increment()
    }
}
