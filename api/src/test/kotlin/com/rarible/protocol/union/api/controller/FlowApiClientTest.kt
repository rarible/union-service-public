package com.rarible.protocol.union.api.controller

import com.rarible.protocol.flow.nft.api.client.FlowNftItemControllerApi
import com.rarible.protocol.union.api.controller.test.IntegrationTest
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@IntegrationTest
class FlowApiClientTest {

    @Autowired
    lateinit var flowNftItemApi: FlowNftItemControllerApi

    @Test
    internal fun contextLoaded() {
       Assertions.assertNotNull(flowNftItemApi)
    }
}
