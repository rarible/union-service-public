package com.rarible.protocol.union.integration.immutablex.client.activity

import org.springframework.web.util.UriBuilder

class TradeQueryBuilder(
    builder: UriBuilder
) : ActivityQueryBuilder(
    builder.path("/trades")
) {

    override val tokenIdField = "party_b_token_id"
    override val tokenField = "party_b_token_address"

    override val timestampMaxField = "max_timestamp"
    override val timestampMinField = "min_timestamp"

}