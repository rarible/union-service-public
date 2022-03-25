package com.rarible.protocol.union.search.core.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding


@ConstructorBinding
@ConfigurationProperties(prefix = "search")
data class SearchProperties(
    val host: String
)