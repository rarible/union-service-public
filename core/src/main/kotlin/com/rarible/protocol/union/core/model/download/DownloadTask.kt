package com.rarible.protocol.union.core.model.download

import java.time.Instant

data class DownloadTask(
    // Task ID, the same as entity ID related to the task
    val id: String,
    // Pipeline should be used to process task (in terms of Kafka - name of topic)
    val pipeline: String,
    // Execute this task without any debouncing/status checks
    val force: Boolean,
    // Date when task has been created
    val scheduledAt: Instant,
)