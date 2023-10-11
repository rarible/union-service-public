package com.rarible.protocol.union.core.model

import java.math.BigInteger

sealed class UnionCollectionTokenId {
    abstract val tokenId: BigInteger
}

data class UnionDefaultCollectionTokenId(
    override val tokenId: BigInteger
) : UnionCollectionTokenId()

data class UnionEthCollectionTokenId(
    override val tokenId: BigInteger,
    val signature: UnionCollectionTokenIdSignature
) : UnionCollectionTokenId()

data class UnionCollectionTokenIdSignature(
    val v: Int,
    val r: String,
    val s: String
)
