package com.rarible.protocol.union.enrichment.meta.simplehash

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
@JsonIgnoreProperties(ignoreUnknown = true)
data class SimpleHashCollection(
    val name: String?,
    val description: String?,
    val imageUrl: String?,
    val bannerImageUrl: String?,
)
