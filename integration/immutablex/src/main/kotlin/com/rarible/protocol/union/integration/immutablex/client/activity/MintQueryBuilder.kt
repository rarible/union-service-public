package com.rarible.protocol.union.integration.immutablex.client.activity

import org.springframework.web.util.UriBuilder

class MintQueryBuilder(
    builder: UriBuilder
) : ActivityQueryBuilder(
    builder.path("/mints")
) {

    override val tokenIdField: String = "token_id"
    override val tokenField: String = "token_address"

    // Incorrect description in API https://docs.x.immutable.com/reference/#/operations/listMints
    //override val timestampMaxField: String = "updated_max_timestamp"
    //override val timestampMinField: String = "updated_min_timestamp"

    override val timestampMaxField: String = "max_timestamp"
    override val timestampMinField: String = "min_timestamp"

}