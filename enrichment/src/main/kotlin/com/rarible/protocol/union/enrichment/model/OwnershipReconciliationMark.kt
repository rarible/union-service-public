package com.rarible.protocol.union.enrichment.model

import com.rarible.protocol.union.dto.BlockchainDto
import org.springframework.data.annotation.AccessType
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Transient
import org.springframework.data.mongodb.core.mapping.Document
import java.math.BigInteger
import java.time.Instant

@Document("ownership_reconciliation_mark")
data class OwnershipReconciliationMark(
    val blockchain: BlockchainDto,
    val token: String,
    val tokenId: BigInteger,
    val owner: String,

    val lastUpdatedAt: Instant
) {

    @Transient
    private val _id: ShortOwnershipId = ShortOwnershipId(blockchain, token, tokenId, owner)

    @get:Id
    @get:AccessType(AccessType.Type.PROPERTY)
    var id: ShortOwnershipId
        get() = _id
        set(_) {}
}