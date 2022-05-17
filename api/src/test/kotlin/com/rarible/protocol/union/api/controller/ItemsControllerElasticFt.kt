package com.rarible.protocol.union.api.controller

import com.rarible.core.common.nowMillis
import com.rarible.protocol.dto.NftItemIdsDto
import com.rarible.protocol.union.api.client.ItemControllerApi
import com.rarible.protocol.union.api.controller.test.AbstractIntegrationTest
import com.rarible.protocol.union.api.controller.test.IntegrationTest
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.continuation.page.PageSize
import com.rarible.protocol.union.enrichment.repository.search.EsItemRepository
import com.rarible.protocol.union.enrichment.test.data.randomEsItem
import com.rarible.protocol.union.integration.ethereum.data.randomEthNftItemDto
import io.mockk.coEvery
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.TestPropertySource
import reactor.kotlin.core.publisher.toFlux
import scalether.domain.Address
import java.math.BigInteger
import java.time.Instant

@FlowPreview
@IntegrationTest
@TestPropertySource(properties = ["common.feature-flags.enableItemQueriesToElasticSearch=true"])
class ItemsControllerElasticFt : AbstractIntegrationTest() {

    private val continuation: String? = null
    private val defaultSize = PageSize.ITEM.default

    @Autowired
    lateinit var itemControllerClient: ItemControllerApi

    @Autowired
    private lateinit var repository: EsItemRepository

    @BeforeEach
    fun setUp() = runBlocking {
        repository.deleteAll()
    }

    @Test
    fun `get all activities`() = runBlocking<Unit> {

        val blockchains = listOf(
            BlockchainDto.ETHEREUM,
            BlockchainDto.POLYGON,
            BlockchainDto.FLOW,
            BlockchainDto.SOLANA,
            BlockchainDto.TEZOS
        )
        val now = Instant.now()
        val contract = Address.ONE().toString()
        val ethItemId1 = ItemIdDto(BlockchainDto.ETHEREUM, contract, BigInteger.valueOf(1))
        val ethItem1 = randomEthNftItemDto(ethItemId1)
        val ethEsItem1 = randomEsItem().copy(itemId = ethItemId1.toString(), blockchain = BlockchainDto.ETHEREUM, lastUpdatedAt = now.plusMillis(-1))
        repository.save(ethEsItem1)
        val ethItemId2 = ItemIdDto(BlockchainDto.ETHEREUM, contract, BigInteger.valueOf(2))
        val ethItem2 = randomEthNftItemDto(ethItemId2)
        val ethEsItem2 = randomEsItem().copy(itemId = ethItemId2.toString(), blockchain = BlockchainDto.ETHEREUM, lastUpdatedAt = now.plusMillis(-2))
        repository.save(ethEsItem2)
        val ethItemId3 = ItemIdDto(BlockchainDto.ETHEREUM, contract, BigInteger.valueOf(3))
        val ethItem3 = randomEthNftItemDto(ethItemId3)
        val ethEsItem3 = randomEsItem().copy(itemId = ethItemId3.toString(), blockchain = BlockchainDto.ETHEREUM, lastUpdatedAt = now.plusMillis(-3))
        repository.save(ethEsItem3)
        val ethItemId4 = ItemIdDto(BlockchainDto.ETHEREUM, contract, BigInteger.valueOf(4))
        val ethItem4 = randomEthNftItemDto(ethItemId4)
        val ethEsItem4 = randomEsItem().copy(itemId = ethItemId4.toString(), blockchain = BlockchainDto.ETHEREUM, lastUpdatedAt = now.plusMillis(-4))
        repository.save(ethEsItem4)
        val ethItemId5 = ItemIdDto(BlockchainDto.ETHEREUM, contract, BigInteger.valueOf(5))
        val ethItem5 = randomEthNftItemDto(ethItemId5)
        val ethEsItem5 = randomEsItem().copy(itemId = ethItemId5.toString(), blockchain = BlockchainDto.ETHEREUM, lastUpdatedAt = now.plusMillis(-5))
        repository.save(ethEsItem5)
        val ethItemId6 = ItemIdDto(BlockchainDto.ETHEREUM, contract, BigInteger.valueOf(6))
        val ethItem6 = randomEthNftItemDto(ethItemId6)
        val ethEsItem6 = randomEsItem().copy(itemId = ethItemId6.toString(), blockchain = BlockchainDto.ETHEREUM, lastUpdatedAt = now.plusMillis(-6))
        repository.save(ethEsItem6)

        val polygonItemId1 = ItemIdDto(BlockchainDto.POLYGON, contract, BigInteger.valueOf(1))
        val polygonItem1 = randomEthNftItemDto(polygonItemId1)
        val polygonEsItem1 = randomEsItem().copy(itemId = polygonItemId1.toString(), blockchain = BlockchainDto.POLYGON, lastUpdatedAt = now.plusMillis(-7))
        repository.save(polygonEsItem1)
        val polygonItemId2 = ItemIdDto(BlockchainDto.POLYGON, contract, BigInteger.valueOf(2))
        val polygonItem2 = randomEthNftItemDto(polygonItemId2)
        val polygonEsItem2 = randomEsItem().copy(itemId = polygonItemId2.toString(), blockchain = BlockchainDto.POLYGON, lastUpdatedAt = now.plusMillis(-8))
        repository.save(polygonEsItem2)
        val polygonItemId3 = ItemIdDto(BlockchainDto.POLYGON, contract, BigInteger.valueOf(3))
        val polygonItem3 = randomEthNftItemDto(polygonItemId3)
        val polygonEsItem3 = randomEsItem().copy(itemId = polygonItemId3.toString(), blockchain = BlockchainDto.POLYGON, lastUpdatedAt = now.plusMillis(-9))
        repository.save(polygonEsItem3)
        val polygonItemId4 = ItemIdDto(BlockchainDto.POLYGON, contract, BigInteger.valueOf(4))
        val polygonItem4 = randomEthNftItemDto(polygonItemId4)
        val polygonEsItem4 = randomEsItem().copy(itemId = polygonItemId4.toString(), blockchain = BlockchainDto.POLYGON, lastUpdatedAt = now.plusMillis(-10))
        repository.save(polygonEsItem4)
        val polygonItemId5 = ItemIdDto(BlockchainDto.POLYGON, contract, BigInteger.valueOf(5))
        val polygonItem5 = randomEthNftItemDto(polygonItemId5)
        val polygonEsItem5 = randomEsItem().copy(itemId = polygonItemId5.toString(), blockchain = BlockchainDto.POLYGON, lastUpdatedAt = now.plusMillis(-11))
        repository.save(polygonEsItem5)
        val polygonItemId6 = ItemIdDto(BlockchainDto.POLYGON, contract, BigInteger.valueOf(6))
        val polygonItem6 = randomEthNftItemDto(polygonItemId6)
        val polygonEsItem6 = randomEsItem().copy(itemId = polygonItemId6.toString(), blockchain = BlockchainDto.POLYGON, lastUpdatedAt = now.plusMillis(-12))
        repository.save(polygonEsItem6)

        // Since all items types specified in request, all of existing clients should be requested
        coEvery {
            testEthereumItemApi.getNftItemsByIds(
                NftItemIdsDto(
                    listOf(
                        ethEsItem1.itemId,
                        ethEsItem2.itemId,
                        ethEsItem3.itemId
                    )
                )
            )
        } returns listOf(ethItem1, ethItem2, ethItem3).toFlux()

        coEvery {
            testEthereumItemApi.getNftItemsByIds(
                NftItemIdsDto(
                    listOf(
                        ethEsItem4.itemId,
                        ethEsItem5.itemId,
                        ethEsItem6.itemId
                    )
                )
            )
        } returns listOf(ethItem4, ethItem5, ethItem6).toFlux()

        coEvery {
            testPolygonItemApi.getNftItemsByIds(
                NftItemIdsDto(
                    listOf(
                        polygonEsItem1.itemId,
                        polygonEsItem2.itemId,
                        polygonEsItem3.itemId
                    )
                )
            )
        } returns listOf(polygonItem1, polygonItem2, polygonItem3).toFlux()

        coEvery {
            testPolygonItemApi.getNftItemsByIds(
                NftItemIdsDto(
                    listOf(
                        polygonEsItem4.itemId,
                        polygonEsItem5.itemId,
                        polygonEsItem6.itemId
                    )
                )
            )
        } returns listOf(polygonItem4, polygonItem5, polygonItem6).toFlux()

        val showDeleted = true
        val size = 3
        val lastUpdatedFrom = nowMillis().minusSeconds(120).toEpochMilli()
        val lastUpdatedTo = nowMillis().plusSeconds(120).toEpochMilli()

        var result = itemControllerClient.getAllItems(
            blockchains, null, size, showDeleted, lastUpdatedFrom, lastUpdatedTo
        ).awaitFirst()

        assertThat(result.items).hasSize(3)
        assertThat(result.items.map { it.id.toString() }).containsAll(
            listOf(
                ethItemId1.toString(),
                ethItemId2.toString(),
                ethItemId3.toString()
            )
        )
        assertThat(result.continuation).isNotNull()

        result = itemControllerClient.getAllItems(
            blockchains, result.continuation, size, showDeleted, lastUpdatedFrom, lastUpdatedTo
        ).awaitFirst()

        assertThat(result.items).hasSize(3)
        assertThat(result.items.map { it.id.toString() }).containsAll(
            listOf(
                ethItemId4.toString(),
                ethItemId5.toString(),
                ethItemId6.toString()
            )
        )
        assertThat(result.continuation).isNotNull()

        result = itemControllerClient.getAllItems(
            blockchains, result.continuation, size, showDeleted, lastUpdatedFrom, lastUpdatedTo
        ).awaitFirst()

        assertThat(result.items).hasSize(3)
        assertThat(result.items.map { it.id.toString() }).containsAll(
            listOf(
                polygonItemId1.toString(),
                polygonItemId2.toString(),
                polygonItemId3.toString()
            )
        )
        assertThat(result.continuation).isNotNull()

        result = itemControllerClient.getAllItems(
            blockchains, result.continuation, size, showDeleted, lastUpdatedFrom, lastUpdatedTo
        ).awaitFirst()

        assertThat(result.items).hasSize(3)
        assertThat(result.items.map { it.id.toString() }).containsAll(
            listOf(
                polygonItemId4.toString(),
                polygonItemId5.toString(),
                polygonItemId6.toString()
            )
        )
        assertThat(result.continuation).isNotNull()

        result = itemControllerClient.getAllItems(
            blockchains, result.continuation, size, showDeleted, lastUpdatedFrom, lastUpdatedTo
        ).awaitFirst()

        assertThat(result.items).hasSize(0)
        assertThat(result.continuation).isEqualTo("")
    }
}
