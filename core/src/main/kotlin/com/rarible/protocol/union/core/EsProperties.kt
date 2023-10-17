package com.rarible.protocol.union.core

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConfigurationProperties("common.es")
@ConstructorBinding
data class EsProperties(
    val itemsTraitsKeysLimit: Int = 200,
    val itemsTraitsValuesLimit: Int = 200,
)
