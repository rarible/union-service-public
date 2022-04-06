package com.rarible.protocol.union.search.core.model

import com.rarible.protocol.union.search.core.ElasticActivity

data class ActivityQueryResult(
    val activities: List<ElasticActivity>,
    val cursor: String?,
)
