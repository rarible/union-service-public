package com.rarible.protocol.union.core.model.elastic

data class FullTextSearch(
    val text: String,
    val fields: List<TextField> = listOf(TextField.NAME)
)

enum class TextField(val esField: String) {
    NAME("name"),
    DESCRIPTION("description"),
    TRAIT_VALUE("traits.value")
}
