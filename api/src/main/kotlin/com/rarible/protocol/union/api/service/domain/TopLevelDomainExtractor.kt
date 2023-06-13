package com.rarible.protocol.union.api.service.domain

object TopLevelDomainExtractor {

    fun extract(domain: String): String? {
        val parts = domain.split(".")
        return if (parts.size > 1) parts.last().lowercase() else null
    }
}