package com.rarible.protocol.union.worker.task.search

data class ChangeAliasTaskParam(
    val indexName: String,
    val tasks: List<String>
)
