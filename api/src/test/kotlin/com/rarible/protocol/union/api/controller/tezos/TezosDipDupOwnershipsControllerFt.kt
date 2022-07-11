package com.rarible.protocol.union.api.controller.tezos

import com.rarible.dipdup.client.exception.DipDupNotFound
import com.rarible.protocol.union.api.client.OwnershipControllerApi
import com.rarible.protocol.union.api.controller.test.AbstractIntegrationTest
import com.rarible.protocol.union.api.controller.test.IntegrationTest
import com.rarible.protocol.union.core.converter.UnionAddressConverter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.OrderIdDto
import com.rarible.protocol.union.dto.OwnershipIdDto
import com.rarible.protocol.union.enrichment.model.ShortOrder
import com.rarible.protocol.union.enrichment.model.ShortOwnership
import com.rarible.protocol.union.enrichment.repository.OwnershipRepository
import com.rarible.protocol.union.integration.tezos.data.randomTzktTokenBalance
import io.mockk.coEvery
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.math.BigInteger
import java.time.Instant

@FlowPreview
@IntegrationTest
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = [
        "application.environment = test",
        "spring.cloud.consul.config.enabled = false",
        "spring.cloud.service-registry.auto-registration.enabled = false",
        "spring.cloud.discovery.enabled = false",
        "logging.logstash.tcp-socket.enabled = false",
        "integration.tezos.dipdup.enabled = true" // turn on dipdup integration
    ]
)
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
            UnionAddressConverter.convert(BlockchainDto.TEZOS,"tz1Uf5tnHetAA7nWzc9TnDMk7jCovp9D8cjB"))
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
            lastUpdatedAt = Instant.now()
        ))

        coEvery {
            testDipDupOrderClient.getOrdersByIds(any())
        } throws DipDupNotFound("")

        val ownership = ownershipControllerApi.getOwnershipById(ownerId.toString()).awaitFirst()
        assertThat(ownership.bestSellOrder).isNull()
    }
}
