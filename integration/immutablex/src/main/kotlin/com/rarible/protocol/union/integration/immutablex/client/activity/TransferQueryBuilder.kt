package com.rarible.protocol.union.integration.immutablex.client.activity

import org.springframework.web.util.UriBuilder

class TransferQueryBuilder(
    builder: UriBuilder
) : ActivityQueryBuilder(
    builder.path("/transfers")
) {

    override val tokenIdField: String = "token_id"
    override val tokenField: String = "token_address"

    override val timestampMaxField = "max_timestamp"
    override val timestampMinField = "min_timestamp"

}