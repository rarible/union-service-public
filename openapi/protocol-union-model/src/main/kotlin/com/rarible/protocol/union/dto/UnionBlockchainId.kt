package com.rarible.protocol.union.dto

interface UnionBlockchainId {

    val blockchain: BlockchainDto

    val value: String

    fun fullId(): String {
        return "${blockchain.name}:${value}"
    }

}