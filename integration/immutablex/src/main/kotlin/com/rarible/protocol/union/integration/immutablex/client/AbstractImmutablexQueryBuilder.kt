package com.rarible.protocol.union.integration.immutablex.client

import org.springframework.web.util.UriBuilder
import java.net.URI

abstract class AbstractImmutablexQueryBuilder(
    protected val builder: UriBuilder
) {

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

}