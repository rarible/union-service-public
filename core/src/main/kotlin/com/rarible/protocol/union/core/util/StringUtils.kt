package com.rarible.protocol.union.core.util

fun safeSplit(str: String?): List<String> {
    return str?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
}