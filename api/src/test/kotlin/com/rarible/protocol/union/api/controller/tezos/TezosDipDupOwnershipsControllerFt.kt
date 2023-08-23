package com.rarible.protocol.union.api.controller.tezos

import com.rarible.core.common.nowMillis
import com.rarible.dipdup.client.core.model.Asset
import com.rarible.dipdup.client.exception.DipDupNotFound
import com.rarible.dipdup.client.model.DipDupOrdersPage
import com.rarible.protocol.union.api.client.OwnershipControllerApi
import com.rarible.protocol.union.api.controller.test.AbstractIntegrationTest
import com.rarible.protocol.union.api.controller.test.IntegrationTest
import com.rarible.protocol.union.core.converter.UnionAddressConverter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.OrderIdDto
import com.rarible.protocol.union.dto.OwnershipDto
import com.rarible.protocol.union.dto.OwnershipIdDto
import com.rarible.protocol.union.enrichment.model.ShortOrder
import com.rarible.protocol.union.enrichment.model.ShortOwnership
import com.rarible.protocol.union.enrichment.repository.OwnershipRepository
import com.rarible.protocol.union.integration.tezos.data.randomTzktItemMintActivity
import com.rarible.protocol.union.integration.tezos.data.randomTzktTokenBalance
import com.rarible.tzkt.model.Page
import io.mockk.coEvery
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigInteger

@FlowPreview
@IntegrationTest
class TezosDipDupOwnershipsControllerFt : AbstractIntegrationTest() {

    @Autowired
    lateinit var ownershipControllerApi: OwnershipControllerApi

    @Autowired
    lateinit var ownershipRepository: OwnershipRepository

    @Test
    fun `should return empty best sell order`() = runBlocking<Unit> {
        val ownerId = OwnershipIdDto(BlockchainDto.TEZOS,
            "KT1RJ6PbjHpwc3M5rw5s2Nbmefwbuwbdxton",
            BigInteger("718165"),
            UnionAddressConverter.convert(BlockchainDto.TEZOS, "tz1Uf5tnHetAA7nWzc9TnDMk7jCovp9D8cjB"))
        val tokenBalance = randomTzktTokenBalance(ownerId)

        coEvery {
            tzktOwnershipClient.ownershipById(ownerId.value)
        } returns tokenBalance

        ownershipRepository.save(ShortOwnership(
            blockchain = BlockchainDto.TEZOS,
            itemId = ownerId.getItemId().value,
            owner = ownerId.owner.value,
            bestSellOrder = ShortOrder(
                blockchain = BlockchainDto.TEZOS,
                id = "f56752e1f6934894ccf8fe52bc9357cd8e1b6a7171a1ca8b633e1b89711176e7",
                platform = "RARIBLE",
                makeStock = BigInteger.ONE,
                dtoId = OrderIdDto(BlockchainDto.TEZOS, "f56752e1f6934894ccf8fe52bc9357cd8e1b6a7171a1ca8b633e1b89711176e7"),
                makePrice = null,
                takePrice = null
            ),
            bestSellOrders = emptyMap(),
            originOrders = emptySet(),
            multiCurrency = false,
            source = null,
            lastUpdatedAt = nowMillis()
        ))

        coEvery {
            testDipDupOrderClient.getOrdersByIds(any())
        } throws DipDupNotFound("")

        val ownership = ownershipControllerApi.getOwnershipById(ownerId.toString()).awaitFirst()
        assertThat(ownership.bestSellOrder).isNull()
    }

    @Test
    fun `should reconcile ownership with legacy order`() = runBlocking<Unit> {
        val ownerId = OwnershipIdDto(BlockchainDto.TEZOS,
            "KT1RJ6PbjHpwc3M5rw5s2Nbmefwbuwbdxton",
            BigInteger("718165"),
            UnionAddressConverter.convert(BlockchainDto.TEZOS, "tz1Uf5tnHetAA7nWzc9TnDMk7jCovp9D8cjB"))
        val tokenBalance = randomTzktTokenBalance(ownerId)

        coEvery {
            tzktOwnershipClient.ownershipById(ownerId.value)
        } returns tokenBalance

        ownershipRepository.save(ShortOwnership(
            blockchain = BlockchainDto.TEZOS,
            itemId = ownerId.getItemId().value,
            owner = ownerId.owner.value,
            bestSellOrder = ShortOrder(
                blockchain = BlockchainDto.TEZOS,
                id = "f56752e1f6934894ccf8fe52bc9357cd8e1b6a7171a1ca8b633e1b89711176e7",
                platform = "RARIBLE",
                makeStock = BigInteger.ONE,
                dtoId = OrderIdDto(BlockchainDto.TEZOS, "f56752e1f6934894ccf8fe52bc9357cd8e1b6a7171a1ca8b633e1b89711176e7"),
                makePrice = null,
                takePrice = null
            ),
            bestSellOrders = emptyMap(),
            originOrders = emptySet(),
            multiCurrency = false,
            source = null,
            lastUpdatedAt = nowMillis()
        ))

        coEvery {
            testDipDupOrderClient.getSellOrdersCurrenciesByItem(any(), any())
        } returns listOf(Asset.XTZ())

        coEvery {
            testDipDupOrderClient.getOrdersByItem(any(), any(), any(), any(), any(), any(), any(), any(), any())
        } returns DipDupOrdersPage(emptyList(), null)

        coEvery {
            tzktTokenActivityClient.getActivitiesByItem(any(), any(), any(), any(), any(), any())
        } returns Page(listOf(randomTzktItemMintActivity("123")), null)

        val uri = "$baseUri/v0.1/refresh/ownership/${ownerId.fullId()}/reconcile"
        val result = testRestTemplate.postForEntity(uri, null, OwnershipDto::class.java).body!!

        assertThat(result).isNotNull
    }
}
