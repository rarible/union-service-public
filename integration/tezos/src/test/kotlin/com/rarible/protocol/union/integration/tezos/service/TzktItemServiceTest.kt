package com.rarible.protocol.union.integration.tezos.service

import com.rarible.protocol.tezos.api.client.NftItemControllerApi
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.integration.tezos.dipdup.service.TzktItemServiceImpl
import com.rarible.tzkt.client.TokenClient
import com.rarible.tzkt.model.Alias
import com.rarible.tzkt.model.Page
import com.rarible.tzkt.model.Token
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant.now
import java.time.ZoneOffset

class TzktItemServiceTest {

    private val itemControllerApi: NftItemControllerApi = mockk()
    private val tokenClient: TokenClient = mockk()


    private val tzktItemService = TzktItemServiceImpl(tokenClient, mockk())
    private val service = TezosItemService(itemControllerApi, tzktItemService)

    @BeforeEach
    fun beforeEach() {
        clearMocks(itemControllerApi)
        clearMocks(tokenClient)
    }

    @Test
    fun `get tzkt item by id`() = runBlocking<Unit> {
        val itemId = "test:123"
        val tzktToken = tzktToken(itemId)
        coEvery { tokenClient.token(itemId) } returns tzktToken

        val item = service.getItemById(itemId)
        assertThat(item.id).isEqualTo(ItemIdDto(BlockchainDto.TEZOS, itemId))
        assertThat(item.deleted).isFalse()
    }

    @Test
    fun `get tzkt items by ids`() = runBlocking<Unit> {
        val itemId = "test:123"
        val tzktToken = tzktToken(itemId)
        coEvery { tokenClient.tokens(listOf(itemId)) } returns listOf(tzktToken)

        val items = service.getItemsByIds(listOf(itemId))
        assertThat(items).hasSize(1)
    }

    @Test
    fun `get tzkt itemsAll`() = runBlocking<Unit> {
        val itemId = "test:123"
        val tzktToken = tzktToken(itemId)
        val continuation = "continuation"
        coEvery { tokenClient.tokens(100, null, true) } returns Page(listOf(tzktToken), continuation)

        val page = service.getAllItems(
            continuation = null,
            size = 100,
            showDeleted = true,
            lastUpdatedFrom = null,
            lastUpdatedTo = null
        )
        assertThat(page.entities).hasSize(1)
        assertThat(page.continuation).isEqualTo(continuation)
    }

    fun tzktToken(itemId: String) = Token(
        id = 1,
        contract = Alias(
            alias = "test name",
            address = "test"
        ),
        tokenId = "123",
        firstTime = now().atOffset(ZoneOffset.UTC),
        lastTime = now().atOffset(ZoneOffset.UTC),
        totalSupply = "1",
        transfersCount = 1,
        balancesCount = 1,
        holdersCount = 1
    )
}
