package com.rarible.protocol.union.listener.job

import com.rarible.protocol.union.enrichment.converter.ShortOrderConverter
import com.rarible.protocol.union.enrichment.repository.DefaultOwnershipRepository
import com.rarible.protocol.union.enrichment.repository.OwnershipRoutedRepository
import com.rarible.protocol.union.enrichment.repository.legacy.LegacyOwnershipRepository
import com.rarible.protocol.union.enrichment.test.data.randomShortOwnership
import com.rarible.protocol.union.enrichment.test.data.randomUnionBidOrderDto
import com.rarible.protocol.union.enrichment.test.data.randomUnionSellOrderDto
import com.rarible.protocol.union.listener.test.AbstractIntegrationTest
import com.rarible.protocol.union.listener.test.IntegrationTest
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@IntegrationTest
class OwnershipRoutedRepositoryIt : AbstractIntegrationTest() {

    lateinit var ownershipRoutedRepository: OwnershipRoutedRepository

    @Autowired
    lateinit var legacyOwnershipRepository: LegacyOwnershipRepository

    @Autowired
    lateinit var defaultOwnershipRepository: DefaultOwnershipRepository

    @Test
    fun `save - legacy is true`() = runBlocking<Unit> {
        ownershipRoutedRepository = OwnershipRoutedRepository(
            legacyOwnershipRepository, defaultOwnershipRepository, true
        )
        val bid = ShortOrderConverter.convert(randomUnionBidOrderDto())
        val sell = ShortOrderConverter.convert(randomUnionSellOrderDto())
        val ownership = randomShortOwnership().copy(
            bestSellOrder = sell,
            bestSellOrders = mapOf("123" to sell),
            multiCurrency = true
        )

        ownershipRoutedRepository.save(ownership)

        val legacy = legacyOwnershipRepository.get(ownership.id)!!.copy(version = null)
        val actual = defaultOwnershipRepository.get(ownership.id)!!.copy(version = null)

        assertThat(legacy).isEqualTo(actual)
        assertThat(legacy).isEqualTo(ownership.copy(version = null))
    }

    @Test
    fun `save - legacy is false`() = runBlocking<Unit> {
        ownershipRoutedRepository = OwnershipRoutedRepository(
            legacyOwnershipRepository, defaultOwnershipRepository, false
        )
        val ownership = randomShortOwnership()

        ownershipRoutedRepository.save(ownership)

        val legacy = legacyOwnershipRepository.get(ownership.id)
        val actual = defaultOwnershipRepository.get(ownership.id)!!.copy(version = null)

        assertThat(legacy).isNull()
        assertThat(actual).isEqualTo(ownership.copy(version = null))
    }

    @Test
    fun `delete - legacy is true`() = runBlocking<Unit> {
        ownershipRoutedRepository = OwnershipRoutedRepository(
            legacyOwnershipRepository, defaultOwnershipRepository, true
        )
        val ownership = randomShortOwnership()

        ownershipRoutedRepository.save(ownership)

        assertThat(defaultOwnershipRepository.get(ownership.id)).isNotNull
        assertThat(legacyOwnershipRepository.get(ownership.id)).isNotNull

        ownershipRoutedRepository.delete(ownership.id)

        assertThat(defaultOwnershipRepository.get(ownership.id)).isNull()
        assertThat(legacyOwnershipRepository.get(ownership.id)).isNull()
    }

    @Test
    fun `delete - legacy is false`() = runBlocking<Unit> {
        ownershipRoutedRepository = OwnershipRoutedRepository(
            legacyOwnershipRepository, defaultOwnershipRepository, false
        )
        val ownership = randomShortOwnership()

        ownershipRoutedRepository.save(ownership)

        assertThat(defaultOwnershipRepository.get(ownership.id)).isNotNull
        assertThat(legacyOwnershipRepository.get(ownership.id)).isNull()

        ownershipRoutedRepository.delete(ownership.id)

        assertThat(defaultOwnershipRepository.get(ownership.id)).isNull()
        assertThat(legacyOwnershipRepository.get(ownership.id)).isNull()
    }

}
