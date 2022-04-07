package com.rarible.protocol.union.search.reindexer.config



import com.rarible.protocol.union.dto.ActivityTypeDto
import com.rarible.protocol.union.dto.BlockchainDto
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConstructorBinding
@ConfigurationProperties(prefix = "reindexer")
data class SearchReindexerProperties(
    val activityTasks: List<ActivityTaskConfig> = allTasks()
)

data class ActivityTaskConfig(
    val blockchainDto: BlockchainDto,
    val type: ActivityTypeDto,
    val enabled: Boolean = false
) {
    fun taskParam() = "ACTIVITY_${blockchainDto}_$type"
}

fun allTasks(): MutableList<ActivityTaskConfig> = run {
    val list = mutableListOf<ActivityTaskConfig>()
    BlockchainDto.values().forEach { blockchain ->
        ActivityTypeDto.values().forEach { activity ->
            list.add(ActivityTaskConfig(blockchain, activity, true))
        }
    }
    list
}