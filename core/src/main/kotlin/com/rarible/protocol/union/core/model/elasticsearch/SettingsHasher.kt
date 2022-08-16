package com.rarible.protocol.union.core.model.elasticsearch

import java.math.BigInteger
import java.security.MessageDigest

object SettingsHasher {
    fun md5(input: String): String {
        val md = MessageDigest.getInstance("MD5")
        return BigInteger(1, md.digest(input.toByteArray())).toString(16).padStart(32, '0')
    }
}