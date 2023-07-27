package com.rarible.protocol.union.worker

import com.rarible.protocol.nft.api.client.NftItemControllerApi
import com.rarible.protocol.union.integration.ethereum.mock.EthItemControllerApiMock
import com.rarible.protocol.union.integration.ethereum.mock.EthOrderControllerApiMock
import io.mockk.clearMocks
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier

abstract class AbstractIntegrationTest {

    @Autowired
    @Qualifier("ethereum.item.api")
    lateinit var testEthereumItemApi: NftItemControllerApi

    @Autowired
    @Qualifier("ethereum.order.api")
    lateinit var testEthereumOrderApi: com.rarible.protocol.order.api.client.OrderControllerApi

    lateinit var ethereumItemControllerApiMock: EthItemControllerApiMock
    lateinit var ethereumOrderControllerApiMock: EthOrderControllerApiMock

    @BeforeEach
    fun beforeEachTest() {
        clearMocks(
            testEthereumItemApi,
            testEthereumOrderApi,
        )

        ethereumItemControllerApiMock = EthItemControllerApiMock(testEthereumItemApi)
        ethereumOrderControllerApiMock = EthOrderControllerApiMock(testEthereumOrderApi)
    }
}
