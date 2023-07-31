package com.rarible.protocol.union.core.model

sealed class RestrictionRule

data class RestrictionApiRule(
    val method: Method,
    val uriTemplate: String,
    val bodyTemplate: String? = null
) : RestrictionRule() {

    enum class Method {
        GET,
        POST
    }
}
