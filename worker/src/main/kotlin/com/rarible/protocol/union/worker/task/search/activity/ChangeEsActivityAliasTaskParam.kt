package com.rarible.protocol.union.worker.task.search.activity


data class ChangeEsActivityAliasTaskParam(
    val indexName: String,
    val tasks: List<ActivityTaskParam>
)