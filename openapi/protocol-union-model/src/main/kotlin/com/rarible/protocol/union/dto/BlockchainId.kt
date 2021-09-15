package com.rarible.protocol.union.dto

interface BlockchainId {

    fun blockchainName(): String

    val value: String

    fun fullId(): String {
        return "${blockchainName()}:${value}"
    }

}