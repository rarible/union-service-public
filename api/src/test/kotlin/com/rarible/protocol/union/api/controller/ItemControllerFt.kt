package com.rarible.protocol.union.api.controller

import com.rarible.protocol.union.api.client.ItemControllerApi
import com.rarible.protocol.union.api.controller.test.IntegrationTest
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.reactive.function.client.WebClientResponseException

@IntegrationTest
class ItemControllerFt {

    @Autowired
    lateinit var itemControllerClient: ItemControllerApi

    @Test
    fun test() = runBlocking<Unit> {
        try {
            itemControllerClient.getItemById("abc", false)
        } catch (e: WebClientResponseException) {
            Assertions.assertEquals(500, e.rawStatusCode)
        }
    }

}