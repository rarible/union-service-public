package com.rarible.protocol.union.integration.tezos.service

import com.rarible.core.test.data.randomString
import com.rarible.protocol.tezos.api.client.NftOwnershipControllerApi
import com.rarible.protocol.tezos.dto.NftOwnershipsDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.BlockchainGroupDto
import com.rarible.protocol.union.dto.OwnershipIdDto
import com.rarible.protocol.union.dto.UnionAddress
import com.rarible.protocol.union.integration.tezos.data.randomTezosOwnershipDto
import com.rarible.protocol.union.integration.tezos.data.randomTezosOwnershipId
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
import reactor.kotlin.core.publisher.toMono
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
        val continuation = randomString()
        val size = 42
        val newContinuation = randomString()
        val id = randomTezosOwnershipId()
        val ownership = randomTezosOwnershipDto(id)
        coEvery {
            ownershipControllerApi.getNftAllOwnerships(any(), any())
        } returns NftOwnershipsDto(2, newContinuation, listOf(ownership)).toMono()

        // when
        val actual = service.getOwnershipsAll(continuation, size)

        // then
        assertThat(actual.continuation).isEqualTo(newContinuation)
        assertThat(actual.entities).hasSize(1)
        assertThat(actual.entities.first().id.value).isEqualTo(id.value)
        coVerify {
            ownershipControllerApi.getNftAllOwnerships(size, continuation)
        }
        confirmVerified(ownershipControllerApi)
    }


    fun ownership() = TokenBalance(
        id = 1,
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
