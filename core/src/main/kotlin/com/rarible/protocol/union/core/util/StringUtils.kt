package com.rarible.protocol.union.core.util

import java.security.MessageDigest

fun safeSplit(str: String?): List<String> {
    return str?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
}

fun trimToLength(str: String?, maxLength: Int, suffix: String? = null): String? {
    if (str == null || str.length <= maxLength) {
        return str
    }
    val safeSuffix = suffix ?: ""
    val trimmed = StringBuilder(maxLength + safeSuffix.length)
        .append(str.substring(0, maxLength))
        .append(safeSuffix)

    return trimmed.toString()
}

fun capitalise(str: String) {
    str.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
}

fun String.sha2(): String {
    val byteArray = this.toByteArray()
    val messageDigest = MessageDigest.getInstance("SHA-256")
    val hash = messageDigest.digest(byteArray)
    return hash.toHex()
}

private fun ByteArray.toHex(): String = joinToString(separator = "") { "%02x".format(it) }
