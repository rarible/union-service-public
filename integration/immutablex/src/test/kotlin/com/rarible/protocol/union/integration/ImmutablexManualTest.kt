package com.rarible.protocol.union.integration

import com.rarible.protocol.union.integration.immutablex.client.ImmutablexApiClient
import com.rarible.protocol.union.integration.immutablex.client.ImmutablexWebClientFactory

abstract class ImmutablexManualTest {

    protected val webClient = ImmutablexWebClientFactory.createClient(
        "https://api.ropsten.x.immutable.com/v1",
        null
    )

    protected val client = ImmutablexApiClient(webClient)

}