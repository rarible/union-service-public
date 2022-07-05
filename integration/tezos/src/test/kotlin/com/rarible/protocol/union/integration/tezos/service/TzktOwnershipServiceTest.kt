package com.rarible.protocol.union.integration.tezos.service

import com.rarible.core.test.data.randomInt
import com.rarible.protocol.tezos.api.client.NftOwnershipControllerApi
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.BlockchainGroupDto
import com.rarible.protocol.union.dto.OwnershipIdDto
import com.rarible.protocol.union.dto.UnionAddress
import com.rarible.protocol.union.integration.tezos.dipdup.service.TzktOwnershipServiceImpl
import com.rarible.tzkt.client.OwnershipClient
import com.rarible.tzkt.model.Alias
import com.rarible.tzkt.model.Page
import com.rarible.tzkt.model.TokenBalance
import com.rarible.tzkt.model.TokenInfo
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant.now
import java.time.ZoneOffset

class TzktOwnershipServiceTest {

    private val ownershipControllerApi: NftOwnershipControllerApi = mockk()
    private val ownershipClient: OwnershipClient = mockk()


    private val tzktOwnershipService = TzktOwnershipServiceImpl(ownershipClient)
    private val service = TezosOwnershipService(ownershipControllerApi, tzktOwnershipService)

    @BeforeEach
    fun beforeEach() {
        clearMocks(ownershipControllerApi)
        clearMocks(ownershipClient)
    }

    @Test
    fun `get tzkt ownership by id`() = runBlocking<Unit> {
        val itemId = "tz2QH8sqmgnFajFb5vN6b9KaDmd4ht2yGv6d:123"
        val owner = "tz2QH8sqmgnFajFb5vN6b9KaDmd4ht2yGv6d"
        val ownershipId = "$itemId:$owner"
        val tzktOwnership = ownership()
        coEvery { ownershipClient.ownershipById(ownershipId) } returns tzktOwnership

        val item = service.getOwnershipById(ownershipId)
        assertThat(item.id).isEqualTo(OwnershipIdDto(BlockchainDto.TEZOS, itemId, UnionAddress(BlockchainGroupDto.TEZOS, owner)))
    }

    @Test
    fun `get tzkt itemsAll`() = runBlocking<Unit> {
        val tzktOwnership = ownership()
        val itemId = "tz2QH8sqmgnFajFb5vN6b9KaDmd4ht2yGv6d:123"
        val continuation = "continuation"
        coEvery { ownershipClient.ownershipsByToken(itemId, 100, null, true) } returns Page(listOf(tzktOwnership), continuation)

        val page = service.getOwnershipsByItem(
            itemId = itemId,
            continuation = null,
            size = 100
        )
        assertThat(page.entities).hasSize(1)
        assertThat(page.continuation).isEqualTo(continuation)
    }

    @Test
    fun `should get all ownerships`() = runBlocking<Unit> {
        // given
        val continuation = "30"
        val size = 2
        val tzktOwnership1 = ownership(20)
        val tzktOwnership2 = ownership(10)
        coEvery {
            ownershipClient.ownershipsAll(any(), any())
        } returns Page(
            continuation = tzktOwnership2.id.toString(),
            items = listOf(tzktOwnership1, tzktOwnership2)
        )

        // when
        val actual = service.getOwnershipsAll(continuation, size)

        // then
        assertThat(actual.continuation).isEqualTo(tzktOwnership2.id.toString())
        assertThat(actual.entities).hasSize(2)
        coVerify {
            ownershipClient.ownershipsAll(continuation, size)
        }
        confirmVerified(ownershipClient)
    }


    fun ownership() = ownership(randomInt())
    fun ownership(id: Int) = TokenBalance(
        id = id,
        account = Alias(
            address = "tz2QH8sqmgnFajFb5vN6b9KaDmd4ht2yGv6d"
        ),
        token = TokenInfo(
            contract = Alias(
                address = "tz2QH8sqmgnFajFb5vN6b9KaDmd4ht2yGv6d"
            ),
            tokenId = "123"
        ),
        balance = "1",
        firstTime = now().atOffset(ZoneOffset.UTC),
        lastTime = now().atOffset(ZoneOffset.UTC),
        transfersCount = 1,
        firstLevel = 1,
        lastLevel = 1
    )
}
