package com.rarible.protocol.union.core

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConfigurationProperties("common.elastic-search")
@ConstructorBinding
data class EsProperties(
    var itemsTraitsKeysLimit: Int = 200,
    var itemsTraitsValuesLimit: Int = 200,
)
