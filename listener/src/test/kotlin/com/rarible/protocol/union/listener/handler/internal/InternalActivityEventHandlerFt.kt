package com.rarible.protocol.union.listener.handler.internal

import com.rarible.core.kafka.KafkaMessage
import com.rarible.core.test.data.randomWord
import com.rarible.protocol.dto.Erc721AssetTypeDto
import com.rarible.protocol.dto.EthActivityEventDto
import com.rarible.protocol.dto.EthAssetTypeDto
import com.rarible.protocol.dto.EventTimeMarksDto
import com.rarible.protocol.dto.NftItemDto
import com.rarible.protocol.dto.OrderActivityDto
import com.rarible.protocol.dto.OrderActivityMatchDto
import com.rarible.protocol.dto.OrderActivityMatchSideDto
import com.rarible.protocol.dto.PartDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.BlockchainGroupDto
import com.rarible.protocol.union.dto.BurnActivityDto
import com.rarible.protocol.union.dto.EthEthereumAssetTypeDto
import com.rarible.protocol.union.dto.ItemLastSaleDto
import com.rarible.protocol.union.dto.UnionAddress
import com.rarible.protocol.union.enrichment.model.ShortItem
import com.rarible.protocol.union.enrichment.repository.ItemRepository
import com.rarible.protocol.union.integration.ethereum.data.randomEthItemBurnActivity
import com.rarible.protocol.union.listener.test.AbstractIntegrationTest
import com.rarible.protocol.union.listener.test.IntegrationTest
import io.daonomic.rpc.domain.Word
import io.mockk.every
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import reactor.core.publisher.Mono
import scalether.domain.Address
import java.math.BigDecimal
import java.math.BigInteger
import java.time.Instant
import java.time.temporal.ChronoUnit

@IntegrationTest
class InternalActivityEventHandlerFt : AbstractIntegrationTest() {

    @Autowired
    private lateinit var itemRepository: ItemRepository

    @Test
    fun `internal activity event`() = runBlocking<Unit> {

        val activity = randomEthItemBurnActivity()

        ethActivityProducer.send(
            KafkaMessage(
                key = activity.id,
                value = EthActivityEventDto(activity, EventTimeMarksDto("test"))
            )
        ).ensureSuccess()

        waitAssert {
            val messages = findActivityUpdates(activity.id, BurnActivityDto::class.java)
            assertThat(messages).hasSize(1)
        }
    }

    @Test
    fun `revert sell activity`() = runBlocking<Unit> {
        val buyer = UnionAddress(blockchainGroup = BlockchainGroupDto.ETHEREUM, value = Address.THREE().toString())
        val seller = UnionAddress(blockchainGroup = BlockchainGroupDto.ETHEREUM, value = Address.TWO().toString())
        val saleDate = Instant.now().truncatedTo(ChronoUnit.SECONDS)
        val itemId = "${Address.ONE()}:1"
        val item = ShortItem(
            blockchain = BlockchainDto.ETHEREUM,
            itemId = itemId,
            totalStock = BigInteger.ZERO,
            bestBidOrders = emptyMap(),
            bestSellOrders = emptyMap(),
            lastUpdatedAt = Instant.now(),
        )
        itemRepository.save(item)

        every { testEthereumItemApi.getNftItemById(itemId) } returns
            Mono.just(
                NftItemDto(
                    contract = Address.ONE(),
                    creators = listOf(PartDto(Address.TWO(), 10000)),
                    id = itemId,
                    lazySupply = BigInteger.ZERO,
                    supply = BigInteger.ONE,
                    tokenId = BigInteger.ONE,
                )
            )

        testItemEventHandler.events.clear()
        ethActivityProducer.send(
            KafkaMessage(
                key = "1",
                value = EthActivityEventDto(
                    createActivityDto(
                        saleDate = saleDate.minus(1, ChronoUnit.HOURS),
                        id = "1",
                        price = BigDecimal.TEN,
                        reverted = false,
                        seller = Address.apply(buyer.value),
                        buyer = Address.apply(seller.value),
                    ),
                    EventTimeMarksDto("test")
                )
            )
        )

        waitAssert {
            val messages = findItemUpdates(itemId)
            assertThat(messages).hasSize(1)
            val updatedItem = messages[0].item
            assertThat(updatedItem.lastSale).isEqualTo(
                ItemLastSaleDto(
                    date = saleDate.minus(1, ChronoUnit.HOURS),
                    price = BigDecimal.TEN,
                    value = BigDecimal.ONE,
                    currency = EthEthereumAssetTypeDto(blockchain = BlockchainDto.ETHEREUM),
                    buyer = seller,
                    seller = buyer,
                )
            )
        }

        testItemEventHandler.events.clear()
        ethActivityProducer.send(
            KafkaMessage(
                key = "1",
                value = EthActivityEventDto(
                    createActivityDto(
                        saleDate = saleDate,
                        id = "2",
                        price = BigDecimal.ONE,
                        reverted = false,
                        seller = Address.apply(seller.value),
                        buyer = Address.apply(buyer.value),
                    ),
                    EventTimeMarksDto("test")
                )
            )
        )

        waitAssert {
            val messages = findItemUpdates(itemId)
            assertThat(messages).hasSize(1)
            val updatedItem = messages[0].item
            assertThat(updatedItem.lastSale).isEqualTo(
                ItemLastSaleDto(
                    date = saleDate,
                    price = BigDecimal.ONE,
                    value = BigDecimal.ONE,
                    currency = EthEthereumAssetTypeDto(blockchain = BlockchainDto.ETHEREUM),
                    buyer = buyer,
                    seller = seller,
                )
            )
        }

        testItemEventHandler.events.clear()
        ethActivityProducer.send(
            KafkaMessage(
                key = "1",
                value = EthActivityEventDto(
                    createActivityDto(
                        saleDate = saleDate,
                        id = "2",
                        price = BigDecimal.ONE,
                        reverted = true,
                        seller = Address.apply(seller.value),
                        buyer = Address.apply(buyer.value),
                    ), EventTimeMarksDto("test")
                )
            )
        )

        waitAssert {
            val messages = findItemUpdates(itemId)
            assertThat(messages).hasSize(1)
            val updatedItem = messages[0].item
            assertThat(updatedItem.lastSale).isEqualTo(
                ItemLastSaleDto(
                    date = saleDate.minus(1, ChronoUnit.HOURS),
                    price = BigDecimal.TEN,
                    value = BigDecimal.ONE,
                    currency = EthEthereumAssetTypeDto(blockchain = BlockchainDto.ETHEREUM),
                    buyer = seller,
                    seller = buyer,
                )
            )
        }
    }

    private fun createActivityDto(
        saleDate: Instant,
        id: String,
        price: BigDecimal,
        reverted: Boolean,
        seller: Address,
        buyer: Address
    ) =
        OrderActivityMatchDto(
            transactionHash = Word.apply(randomWord()),
            blockHash = Word.apply(randomWord()),
            date = saleDate,
            id = id,
            right = OrderActivityMatchSideDto(
                asset = com.rarible.protocol.dto.AssetDto(
                    assetType = Erc721AssetTypeDto(
                        contract = Address.ONE(),
                        tokenId = BigInteger.ONE,
                    ),
                    value = BigInteger.ONE,
                    valueDecimal = BigDecimal.ONE,
                ),
                hash = Word.apply(randomWord()),
                maker = seller,
            ),
            logIndex = 2,
            price = price,
            left = OrderActivityMatchSideDto(
                asset = com.rarible.protocol.dto.AssetDto(
                    assetType = EthAssetTypeDto(),
                    value = BigInteger.ONE,
                    valueDecimal = BigDecimal.ONE,
                ),
                hash = Word.apply(randomWord()),
                maker = buyer,
            ),
            source = OrderActivityDto.Source.RARIBLE,
            type = OrderActivityMatchDto.Type.SELL,
            blockNumber = 2,
            reverted = reverted,
        )
}
