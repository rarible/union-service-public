package com.rarible.protocol.union.enrichment.meta.simplehash

import com.rarible.protocol.union.enrichment.meta.item.ItemMetaService
import com.simplehash.v0.nft
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

class SimpleHashKafkaHandlerTest {

    @Test
    fun `schedule meta update - ok`() = runBlocking {
        val itemMetaService: ItemMetaService = mockk() {
            coEvery { scheduleSimpleHashItemUpdate(any()) } returns Unit
        }
        val handler = SimpleHashKafkaHandler(itemMetaService)
        val event = nft.newBuilder().setNftId("ethereum.test").build()

        handler.handle(listOf(event))

        coVerify(exactly = 1) {
            itemMetaService.scheduleSimpleHashItemUpdate(any())
        }
    }

}