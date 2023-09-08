package com.rarible.protocol.union.listener.repository

import com.rarible.core.common.nowMillis
import com.rarible.core.test.data.randomString
import com.rarible.protocol.union.enrichment.model.Lock
import com.rarible.protocol.union.enrichment.repository.LockRepository
import com.rarible.protocol.union.listener.test.IntegrationTest
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import java.time.Duration

@IntegrationTest
class LockRepositoryIt {

    @Autowired
    lateinit var lockRepository: LockRepository

    @Autowired
    lateinit var template: ReactiveMongoTemplate

    private val resetAfter = Duration.ofMinutes(1)

    @Test
    fun `acquire - ok, doesn't exists`() = runBlocking<Unit> {
        val now = nowMillis()
        val id = randomString()
        val result = lockRepository.acquireLock(id, resetAfter)
        val lock = lockRepository.get(id)!!

        assertThat(result).isTrue()
        assertThat(lock.acquired).isTrue()
        assertThat(lock.acquiredAt).isAfterOrEqualTo(now)
    }

    @Test
    fun `acquire - ok, free`() = runBlocking<Unit> {
        val now = nowMillis()
        val id = randomString()
        lockRepository.acquireLock(id, resetAfter)
        lockRepository.releaseLock(id)

        val result = lockRepository.acquireLock(id, resetAfter)
        val lock = lockRepository.get(id)!!

        assertThat(result).isTrue()
        assertThat(lock.acquired).isTrue()
        assertThat(lock.acquiredAt).isAfterOrEqualTo(now)
    }

    @Test
    fun `acquire - ok, reset timeout`() = runBlocking<Unit> {
        val now = nowMillis()
        val id = randomString()
        template.save(
            Lock(
                id = id,
                acquired = true,
                acquiredAt = nowMillis().minus(resetAfter).minusSeconds(1)
            )
        ).awaitSingle()

        val result = lockRepository.acquireLock(id, resetAfter)
        val lock = lockRepository.get(id)!!

        assertThat(result).isTrue()
        assertThat(lock.acquired).isTrue()
        assertThat(lock.acquiredAt).isAfterOrEqualTo(now)
    }

    @Test
    fun `acquire - failed`() = runBlocking<Unit> {
        val id = randomString()
        lockRepository.acquireLock(id, resetAfter)

        val result = lockRepository.acquireLock(id, resetAfter)

        assertThat(result).isFalse()
    }

    @Test
    fun `release - ok, exists`() = runBlocking<Unit> {
        val id = randomString()
        lockRepository.acquireLock(id, resetAfter)
        lockRepository.releaseLock(id)

        val lock = lockRepository.get(id)!!

        assertThat(lock.acquired).isFalse()
    }
}
