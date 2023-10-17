package com.rarible.protocol.union.api.controller.tezos

import com.rarible.protocol.union.api.client.ItemControllerApi
import com.rarible.protocol.union.api.controller.test.AbstractIntegrationTest
import com.rarible.protocol.union.api.controller.test.IntegrationTest
import com.rarible.protocol.union.dto.SearchEngineDto
import com.rarible.protocol.union.integration.tezos.data.randomTezosAddress
import com.rarible.protocol.union.integration.tezos.data.randomTezosItemId
import com.rarible.protocol.union.integration.tezos.data.randomTzktToken
import com.rarible.tzkt.model.Page
import io.mockk.coEvery
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@FlowPreview
@IntegrationTest
class TezosItemControllerFt : AbstractIntegrationTest() {

    @Autowired
    lateinit var itemControllerClient: ItemControllerApi

    @Test
    fun `should return items by collection`() = runBlocking<Unit> {
        val collectionContract = randomTezosAddress()
        coEvery {
            tzktTokenClient.tokensByCollection(collectionContract.value, 1, null)
        } returns Page(listOf(randomTzktToken(randomTezosAddress().value)), null)

        val page = itemControllerClient.getItemsByCollection(
            collectionContract.fullId(),
            null,
            1,
            SearchEngineDto.LEGACY
        ).awaitSingle()
        assertThat(page.items).hasSize(1)
    }

    @Test
    fun `should return items by owner`() = runBlocking<Unit> {
        val owner = randomTezosAddress()
        coEvery {
            tzktTokenClient.tokensByOwner(owner.value, 1, null)
        } returns Page(listOf(randomTzktToken(randomTezosAddress().value)), null)

        val page = itemControllerClient.getItemsByOwner(
            owner.fullId(),
            emptyList(),
            null,
            1,
            SearchEngineDto.LEGACY
        ).awaitSingle()
        assertThat(page.items).hasSize(1)
    }

    @Test
    fun `should return items by creator`() = runBlocking<Unit> {
        val creator = randomTezosAddress()
        coEvery {
            tzktTokenClient.tokensByCreator(creator.value, 1, null)
        } returns Page(listOf(randomTzktToken(randomTezosAddress().value)), null)

        val page = itemControllerClient.getItemsByCreator(
            creator.fullId(),
            emptyList(),
            null,
            1,
            SearchEngineDto.LEGACY
        ).awaitSingle()
        assertThat(page.items).hasSize(1)
    }

    @Test
    fun `should return item with tags and attributes`() = runBlocking<Unit> {
        val itemId = randomTezosItemId()
        val item = randomTzktToken(itemId.value)
        coEvery {
            tzktTokenClient.token(itemId.value)
        } returns item

        val itemDto = itemControllerClient.getItemById(itemId.fullId()).awaitSingle()
        assertThat(itemDto.id.value).isEqualTo(item.contract!!.address + ":" + item.tokenId)
    }
}
