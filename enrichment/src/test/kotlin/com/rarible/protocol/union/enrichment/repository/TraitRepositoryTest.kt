package com.rarible.protocol.union.enrichment.repository

import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.enrichment.model.EnrichmentCollectionId
import com.rarible.protocol.union.enrichment.model.Trait
import com.rarible.protocol.union.enrichment.test.IntegrationTest
import com.rarible.protocol.union.enrichment.util.TraitUtils
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import scalether.domain.Address

@IntegrationTest
internal class TraitRepositoryTest {
    @Autowired
    private lateinit var traitRepository: TraitRepository

    @Test
    fun `save all`() = runBlocking<Unit> {
        val collectionId1 = EnrichmentCollectionId(
            blockchain = BlockchainDto.ETHEREUM,
            collectionId = Address.ONE().toString()
        )
        val collectionId2 = EnrichmentCollectionId(
            blockchain = BlockchainDto.ETHEREUM,
            collectionId = Address.TWO().toString()
        )
        traitRepository.save(
            Trait(
                collectionId = collectionId1,
                itemsCount = 1,
                key = "key",
                value = "value"
            )
        )

        traitRepository.saveAll(
            listOf(
                Trait(
                    collectionId = collectionId1,
                    itemsCount = 2,
                    key = "key",
                    value = "value"
                ),
                Trait(
                    collectionId = collectionId2,
                    itemsCount = 10,
                    key = "key",
                    value = "value"
                )
            )
        )

        val trait1 = traitRepository.get(TraitUtils.getId(collectionId1, "key", "value"))!!

        assertThat(trait1.itemsCount).isEqualTo(2)

        val trait2 = traitRepository.get(TraitUtils.getId(collectionId2, "key", "value"))!!

        assertThat(trait2.itemsCount).isEqualTo(10)
    }

    @Test
    fun `delete all by collection id`() = runBlocking<Unit> {
        val collectionId1 = EnrichmentCollectionId(
            blockchain = BlockchainDto.ETHEREUM,
            collectionId = Address.ONE().toString()
        )
        val collectionId2 = EnrichmentCollectionId(
            blockchain = BlockchainDto.ETHEREUM,
            collectionId = Address.TWO().toString()
        )
        val trait1 = traitRepository.save(
            Trait(
                collectionId = collectionId1,
                itemsCount = 1,
                key = "key",
                value = "value"
            )
        )
        val trait2 = traitRepository.save(
            Trait(
                collectionId = collectionId2,
                itemsCount = 1,
                key = "key",
                value = "value"
            )
        )

        traitRepository.deleteAllByCollection(collectionId1)

        assertThat(traitRepository.get(trait1.id)).isNull()
        assertThat(traitRepository.get(trait2.id)).isNotNull()
    }
}
