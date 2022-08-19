package com.rarible.protocol.union.integration

import com.rarible.protocol.union.integration.immutablex.client.ImxActivityClient
import com.rarible.protocol.union.integration.immutablex.client.ImxAssetClient
import com.rarible.protocol.union.integration.immutablex.client.ImxCollectionClient
import com.rarible.protocol.union.integration.immutablex.client.ImxOrderClient
import com.rarible.protocol.union.integration.immutablex.client.ImxWebClientFactory
import com.rarible.protocol.union.integration.immutablex.repository.ImxCollectionCreatorRepository
import io.mockk.coEvery
import io.mockk.mockk

abstract class ImxManualTest {

    protected val webClient = ImxWebClientFactory.createClient(
        "https://api.ropsten.x.immutable.com/v1",
        null
    )

    private val chunkSize = 16

    protected val assetClient = ImxAssetClient(webClient, chunkSize)
    protected val activityClient = ImxActivityClient(webClient, chunkSize)
    protected val collectionClient = ImxCollectionClient(webClient, chunkSize)
    protected val orderClient = ImxOrderClient(webClient, chunkSize)

    protected val collectionCreatorRepository: ImxCollectionCreatorRepository = mockk {
        coEvery { getAll(any()) } returns emptyList()
        coEvery { saveAll(any()) } returns Unit
    }

}