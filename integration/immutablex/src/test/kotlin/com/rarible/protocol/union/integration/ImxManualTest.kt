package com.rarible.protocol.union.integration

import com.rarible.protocol.union.integration.immutablex.client.ImxActivityClient
import com.rarible.protocol.union.integration.immutablex.client.ImxAssetClient
import com.rarible.protocol.union.integration.immutablex.client.ImxCollectionClient
import com.rarible.protocol.union.integration.immutablex.client.ImxOrderClient
import com.rarible.protocol.union.integration.immutablex.client.ImxWebClientFactory

abstract class ImxManualTest {

    protected val webClient = ImxWebClientFactory.createClient(
        "https://api.ropsten.x.immutable.com/v1",
        null
    )

    protected val assetClient = ImxAssetClient(webClient)
    protected val activityClient = ImxActivityClient(webClient)
    protected val collectionClient = ImxCollectionClient(webClient)
    protected val orderClient = ImxOrderClient(webClient)

}