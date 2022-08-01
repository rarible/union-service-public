package com.rarible.protocol.union.integration

import com.rarible.protocol.union.core.test.ManualTest
import com.rarible.protocol.union.integration.immutablex.client.ImmutablexApiClient
import com.rarible.protocol.union.integration.immutablex.client.ImmutablexWebClientFactory

// For manual debugging purposes
@ManualTest
abstract class ImmutablexManualTest {

    protected val webClient = ImmutablexWebClientFactory.createClient(
        "https://api.ropsten.x.immutable.com/v1",
        null
    )

    protected val client = ImmutablexApiClient(webClient)

}