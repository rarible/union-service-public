package com.rarible.protocol.union.core.util

fun safeSplit(str: String?): List<String> {
    return str?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
}

fun trimToLength(str: String?, maxLength: Int, suffix: String? = null): String? {
    if (str == null || str.length < maxLength) {
        return str
    }
    val safeSuffix = suffix ?: ""
    val trimmed = StringBuilder(maxLength + safeSuffix.length)
        .append(str.substring(0, maxLength))
        .append(safeSuffix)

    return trimmed.toString()
}