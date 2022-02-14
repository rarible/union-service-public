package com.rarible.protocol.union.listener.job

import com.rarible.core.test.data.randomBigInt
import com.rarible.core.test.data.randomInt
import com.rarible.core.test.data.randomString
import com.rarible.protocol.union.dto.AuctionIdDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.enrichment.converter.ShortOrderConverter
import com.rarible.protocol.union.enrichment.repository.DefaultItemRepository
import com.rarible.protocol.union.enrichment.repository.ItemRoutedRepository
import com.rarible.protocol.union.enrichment.repository.legacy.LegacyItemRepository
import com.rarible.protocol.union.enrichment.test.data.randomShortItem
import com.rarible.protocol.union.enrichment.test.data.randomUnionBidOrderDto
import com.rarible.protocol.union.enrichment.test.data.randomUnionSellOrderDto
import com.rarible.protocol.union.listener.test.AbstractIntegrationTest
import com.rarible.protocol.union.listener.test.IntegrationTest
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@FlowPreview
@IntegrationTest
@Deprecated("Remove after migration")
class ItemRoutedRepositoryIt : AbstractIntegrationTest() {

    lateinit var itemRoutedRepository: ItemRoutedRepository

    @Autowired
    lateinit var legacyItemRepository: LegacyItemRepository

    @Autowired
    lateinit var defaultItemRepository: DefaultItemRepository

    @Test
    fun `save - legacy is true`() = runBlocking<Unit> {
        itemRoutedRepository = ItemRoutedRepository(legacyItemRepository, defaultItemRepository, true)
        val bid = ShortOrderConverter.convert(randomUnionBidOrderDto())
        val sell = ShortOrderConverter.convert(randomUnionSellOrderDto())
        val item = randomShortItem().copy(
            bestSellOrder = sell,
            bestBidOrder = bid,
            bestSellOrders = mapOf("123" to sell),
            bestBidOrders = mapOf("321" to bid),
            sellers = randomInt(),
            totalStock = randomBigInt(),
            auctions = setOf(AuctionIdDto(BlockchainDto.ETHEREUM, randomString())),
            multiCurrency = true
        )

        itemRoutedRepository.save(item)

        val legacy = legacyItemRepository.get(item.id)!!.copy(version = null)
        val actual = defaultItemRepository.get(item.id)!!.copy(version = null)

        assertThat(legacy).isEqualTo(actual)
        assertThat(legacy).isEqualTo(item.copy(version = null))
    }

    @Test
    fun `save - legacy is false`() = runBlocking<Unit> {
        itemRoutedRepository = ItemRoutedRepository(legacyItemRepository, defaultItemRepository, false)
        val item = randomShortItem()

        itemRoutedRepository.save(item)

        val legacy = legacyItemRepository.get(item.id)
        val actual = defaultItemRepository.get(item.id)!!.copy(version = null)

        assertThat(legacy).isNull()
        assertThat(actual).isEqualTo(item.copy(version = null))
    }

    @Test
    fun `delete - legacy is true`() = runBlocking<Unit> {
        itemRoutedRepository = ItemRoutedRepository(legacyItemRepository, defaultItemRepository, true)
        val item = randomShortItem()

        itemRoutedRepository.save(item)

        assertThat(defaultItemRepository.get(item.id)).isNotNull
        assertThat(legacyItemRepository.get(item.id)).isNotNull

        itemRoutedRepository.delete(item.id)

        assertThat(defaultItemRepository.get(item.id)).isNull()
        assertThat(legacyItemRepository.get(item.id)).isNull()
    }

    @Test
    fun `delete - legacy is false`() = runBlocking<Unit> {
        itemRoutedRepository = ItemRoutedRepository(legacyItemRepository, defaultItemRepository, false)
        val item = randomShortItem()

        itemRoutedRepository.save(item)

        assertThat(defaultItemRepository.get(item.id)).isNotNull
        assertThat(legacyItemRepository.get(item.id)).isNull()

        itemRoutedRepository.delete(item.id)

        assertThat(defaultItemRepository.get(item.id)).isNull()
        assertThat(legacyItemRepository.get(item.id)).isNull()
    }

}