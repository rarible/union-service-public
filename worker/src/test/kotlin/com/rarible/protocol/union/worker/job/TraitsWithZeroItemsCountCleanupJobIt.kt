package com.rarible.protocol.union.worker.job

import com.rarible.protocol.union.enrichment.model.Trait
import com.rarible.protocol.union.enrichment.repository.TraitRepository
import com.rarible.protocol.union.enrichment.service.TraitService
import com.rarible.protocol.union.integration.ethereum.data.randomEthCollectionId
import com.rarible.protocol.union.worker.IntegrationTest
import com.rarible.protocol.union.worker.config.TraitsWithZeroItemsCountCleanUpProperties
import com.rarible.protocol.union.worker.config.WorkerProperties
import io.mockk.mockk
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Duration

@IntegrationTest
class TraitsWithZeroItemsCountCleanupJobIt {

    @Autowired
    lateinit var traitRepository: TraitRepository

    @Autowired
    lateinit var traitService: TraitService

    lateinit var job: TraitsWithZeroItemsCountCleanUpJob

    @BeforeEach
    fun beforeEach() = runBlocking<Unit> {
        traitRepository.save(Trait(
            collectionId = randomEthCollectionId(),
            key = "1",
            value = "",
            itemsCount = 1
        ))
        traitRepository.save(Trait(
            collectionId = randomEthCollectionId(),
            key ="2",
            value = "",
            itemsCount = 0
        ))
        job = TraitsWithZeroItemsCountCleanUpJob(
            meterRegistry = mockk(),
            properties = WorkerProperties(
                traitsWithZeroItemsCountCleanUp = TraitsWithZeroItemsCountCleanUpProperties(
                    enabled = true,
                    rate = Duration.ofMillis(1)
                )
            ),
            traitService = traitService
        )
    }

    @Test
    fun `cleanup openSea best sells`() = runBlocking<Unit> {
        val withZero = traitRepository.findWithZeroItemsCount().count()
        assertThat(withZero).isEqualTo(1)

        job.handle()

        val remaining = traitRepository.findWithZeroItemsCount().count()
        assertThat(remaining).isEqualTo(0)
    }
}
