package com.rarible.protocol.union.enrichment.download

import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document("download_task")
data class DownloadTask(
    // Task ID, the same as entity ID related to the task
    val id: String,
    // Entity type
    val type: String,
    // Pipeline should be used to process task (in terms of Kafka - name of topic)
    val pipeline: String,
    // Execute this task without any debouncing/status checks
    val force: Boolean,
    // Where from task was called
    val source: DownloadTaskSource,
    // Date when task has been created
    val priority: Int = 0,
    // Date when task has been created
    val scheduledAt: Instant,
    // Date when task taken to execution
    val startedAt: Instant? = null,
    // Indicates task currently in progress
    val inProgress: Boolean = false
) {

    fun toEvent(): DownloadTaskEvent {
        return DownloadTaskEvent(
            id = id,
            pipeline = pipeline,
            force = force,
            source = source,
            scheduledAt = scheduledAt,
            priority = priority
        )
    }
}

enum class DownloadTaskSource {
    INTERNAL,
    EXTERNAL
}
