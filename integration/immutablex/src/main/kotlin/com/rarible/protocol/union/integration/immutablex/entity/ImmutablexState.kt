package com.rarible.protocol.union.integration.immutablex.entity

import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.FieldType
import org.springframework.data.mongodb.core.mapping.MongoId

@Document("immutablex_state")
data class ImmutablexState(
    @MongoId(FieldType.INT64)
    val id: Long,
    val lastMintCursor: String? = null,
    val lastTransferCursor: String? = null,
    val lastOrderCursor: String? = null,
    val lastDepositCursor: String? = null,
    val lastWithdrawCursor: String? = null,
    val lastTradesCursor: String? = null,
)
