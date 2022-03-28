package com.rarible.protocol.union.search.indexer

import com.rarible.core.daemon.sequential.ConsumerWorker
import com.rarible.protocol.union.search.indexer.test.IntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired


@IntegrationTest
class UnionSearchIndexerAppIntegrationTest {

    @Autowired
    private lateinit var consumerWorkers: List<ConsumerWorker<*>>

    @Test
    fun `should bring context up`() {
        assertThat(consumerWorkers).hasSize(1)
    }

}
