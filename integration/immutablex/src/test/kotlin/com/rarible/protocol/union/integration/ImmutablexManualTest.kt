package com.rarible.protocol.union.integration

import com.rarible.protocol.union.integration.immutablex.client.ImmutablexActivityClient
import com.rarible.protocol.union.integration.immutablex.client.ImmutablexAssetClient
import com.rarible.protocol.union.integration.immutablex.client.ImmutablexCollectionClient
import com.rarible.protocol.union.integration.immutablex.client.ImmutablexOrderClient
import com.rarible.protocol.union.integration.immutablex.client.ImmutablexWebClientFactory

abstract class ImmutablexManualTest {

    protected val webClient = ImmutablexWebClientFactory.createClient(
        "https://api.ropsten.x.immutable.com/v1",
        null
    )

    protected val assetClient = ImmutablexAssetClient(webClient)
    protected val activityClient = ImmutablexActivityClient(webClient)
    protected val collectionClient = ImmutablexCollectionClient(webClient)
    protected val orderClient = ImmutablexOrderClient(webClient)

}