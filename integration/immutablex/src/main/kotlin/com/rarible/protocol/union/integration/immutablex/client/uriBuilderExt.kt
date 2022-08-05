package com.rarible.protocol.union.integration.immutablex.client

import org.springframework.web.util.UriBuilder

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