package com.rarible.protocol.union.api.controller

import com.rarible.protocol.nftorder.api.test.IntegrationTest
import com.rarible.protocol.union.api.client.TestControllerApi
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@IntegrationTest
class TestControllerIt {

    @Autowired
    lateinit var testControllerClient: TestControllerApi

    @Test
    fun test() = runBlocking {
        assertThat(testControllerClient.test.awaitFirst().id).isEqualTo(12)
    }

}