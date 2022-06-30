package com.rarible.protocol.union.integration.tezos.service

import com.rarible.protocol.tezos.api.client.NftCollectionControllerApi
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.integration.tezos.dipdup.service.TzktCollectionServiceImpl
import com.rarible.protocol.union.integration.tezos.entity.TezosCollectionRepository
import com.rarible.tzkt.client.CollectionClient
import com.rarible.tzkt.model.Contract
import com.rarible.tzkt.model.Page
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class TezosCollectionServiceTest {

    private val nftCollectionApi: NftCollectionControllerApi = mockk()
    private val tzktCollectionClient: CollectionClient = mockk()
    private val tezosCollectionRepository: TezosCollectionRepository = mockk()


    private val tzktCollectionService = TzktCollectionServiceImpl(tzktCollectionClient, mockk(), tezosCollectionRepository)
    private val service = TezosCollectionService(nftCollectionApi, mockk(), tzktCollectionService, mockk())

    @BeforeEach
    fun beforeEach() {
        clearMocks(nftCollectionApi)
        clearMocks(tzktCollectionClient)
    }

    @Test
    fun `should return tzkt collections`() = runBlocking<Unit> {
        val address = "KT1Tu6A2NHKwEjdHTTJBys8Pu8K9Eo87P2Vy"
        val continuation = "test"
        coEvery { tzktCollectionClient.collectionsAll(1, null) } returns Page(
            items = listOf(contract(address)),
            continuation = continuation
        )

        val contract = service.getAllCollections(null, 1)

        assertThat(contract.entities).hasSize(1)
        assertThat(contract.continuation).isEqualTo(continuation)
        assertThat(contract.entities.first().id).isEqualTo(CollectionIdDto(BlockchainDto.TEZOS, address))
    }

    @Test
    fun `should return tzkt collections by id`() = runBlocking<Unit> {
        val address = "KT1Tu6A2NHKwEjdHTTJBys8Pu8K9Eo87P2Vy"
        coEvery { tzktCollectionClient.collection(address) } returns contract(address)
        val contract = service.getCollectionById(address)

        assertThat(contract.id).isEqualTo(CollectionIdDto(BlockchainDto.TEZOS, address))
    }

    @Test
    fun `should return tzkt collections by ids`() = runBlocking<Unit> {
        val addresses = listOf("KT1Tu6A2NHKwEjdHTTJBys8Pu8K9Eo87P2Vy")
        coEvery { tzktCollectionClient.collectionsByIds(addresses) } returns addresses.map(::contract)
        val contracts = service.getCollectionsByIds(addresses)

        assertThat(contracts).hasSize(1)
    }

    fun contract(address: String): Contract = Contract(
        type = "contract",
        alias = "",
        balance = 1L,
        address = address,
        tzips = listOf("fa2"),
        kind = "",
        numContracts = 1,
        activeTokensCount = 1,
        tokenBalancesCount = 1,
        tokenTransfersCount = 1,
        numDelegations = 1,
        numOriginations = 1,
        numTransactions = 1,
        numReveals = 1,
        numMigrations = 1
    )

}
