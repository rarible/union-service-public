package com.rarible.protocol.union.worker.job

import com.rarible.core.kafka.KafkaMessage
import com.rarible.core.kafka.KafkaSendResult
import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.protocol.union.core.model.UnionTraitEvent
import com.rarible.protocol.union.enrichment.model.Trait
import com.rarible.protocol.union.enrichment.repository.TraitRepository
import com.rarible.protocol.union.enrichment.service.TraitService
import com.rarible.protocol.union.worker.IntegrationTest
import com.rarible.protocol.union.worker.config.TraitsWithZeroItemsCountCleanUpProperties
import com.rarible.protocol.union.worker.config.WorkerProperties
import com.rarible.protocol.union.worker.test.randomTrait
import io.mockk.coEvery
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import java.time.Duration

@IntegrationTest
class TraitsWithZeroItemsCountCleanupJobIt {

    @Autowired
    lateinit var template: ReactiveMongoTemplate

    @Autowired
    lateinit var traitRepository: TraitRepository

    @Autowired
    lateinit var traitService: TraitService

    @MockK
    private lateinit var producer: RaribleKafkaProducer<UnionTraitEvent>

    lateinit var job: TraitsWithZeroItemsCountCleanUpJob

    @BeforeEach
    fun beforeEach() = runBlocking<Unit> {
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
        val kafkaResult: KafkaSendResult.Success = mockk()
        coEvery { producer.send(any<KafkaMessage<UnionTraitEvent>>()) } returns kafkaResult
        coEvery { kafkaResult.ensureSuccess() } returns kafkaResult
    }

    @Test
    fun `clean up`() = runBlocking<Unit> {
        traitRepository.save(randomTrait())
        traitRepository.save(randomTrait().copy(itemsCount = 0))

        val withZero = findWithZeroItemsCount().count()
        assertThat(withZero).isEqualTo(1)

        job.handle()

        val remaining = findWithZeroItemsCount().count()
        assertThat(remaining).isEqualTo(0)

        coEvery { producer.send(match<KafkaMessage<UnionTraitEvent>> {
            it.value.itemsCount == 0L
        }) }
    }

    suspend fun findWithZeroItemsCount(): Flow<Trait> =
        template.find(
            Query(Criteria().and(Trait::itemsCount.name).lte(0L)),
            Trait::class.java
        ).asFlow()
}
