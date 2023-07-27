package com.rarible.protocol.union.integration.immutablex.client

import org.springframework.web.util.UriBuilder
import java.net.URI

abstract class AbstractImxQueryBuilder(
    protected val builder: UriBuilder,
    protected val path: String
) {

    init {
        builder.path(path)
    }

    protected fun getDefaultPageSize(): Int {
        return 50
    }

    fun pageSize(size: Int?) {
        builder.queryParam("page_size", size ?: getDefaultPageSize())
    }

    fun orderBy(field: String?, direction: String?) {
        direction ?: return
        builder.queryParamNotNull("order_by", field)
        builder.queryParam("direction", direction)
    }

    fun cursor(cursor: String?) {
        builder.queryParamNotNull("cursor", cursor)
    }

    fun build(): URI {
        return this.builder.build()
    }

    override fun toString(): String {
        return build().toString()
    }

    fun UriBuilder.queryParamNotNull(name: String, value: Any?): UriBuilder {
        return value?.let { this.queryParam(name, it) } ?: this
    }

    fun UriBuilder.queryParamNotNull(name: String, value: String?): UriBuilder {
        return if (value.isNullOrBlank()) {
            return this
        } else {
            this.queryParam(name, value)
        }
    }
}
