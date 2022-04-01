package com.rarible.protocol.union.search.reindexer.config



import com.rarible.protocol.union.dto.ActivityTypeDto
import com.rarible.protocol.union.dto.BlockchainDto
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConstructorBinding
@ConfigurationProperties(prefix = "reindexer")
data class SearchReindexerProperties(
    val activityTasks: List<ActivityTaskConfig>
)

data class ActivityTaskConfig(
    val blockchainDto: BlockchainDto,
    val type: ActivityTypeDto,
    val enabled: Boolean = false
) {
    fun taskParam() = "ACTIVITY_${blockchainDto}_$type"
}