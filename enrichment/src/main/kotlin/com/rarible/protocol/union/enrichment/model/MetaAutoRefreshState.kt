package com.rarible.protocol.union.enrichment.model

import com.rarible.core.common.nowMillis
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document("meta_auto_refresh_state")
data class MetaAutoRefreshState(
    @Id
    val id: String,
    val status: MetaAutoRefreshStatus = MetaAutoRefreshStatus.CREATED,
    val createdAt: Instant = nowMillis(),
    val lastRefreshedAt: Instant? = null,
)

enum class MetaAutoRefreshStatus {
    CREATED,
    REFRESHED,
}
