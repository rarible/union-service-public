package com.rarible.protocol.union.search.reindexer.config



import com.rarible.protocol.union.dto.ActivityTypeDto
import com.rarible.protocol.union.dto.BlockchainDto
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConstructorBinding
@ConfigurationProperties(prefix = "reindexer")
class SearchReindexerProperties {
    val activityTasks: List<ActivityTaskConfig> = allTasks()
    val startReindexActivity = false
    val orderTasks: Set<BlockchainDto> = BlockchainDto.values().toSet()
    val startReindexOrder = false
}

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
