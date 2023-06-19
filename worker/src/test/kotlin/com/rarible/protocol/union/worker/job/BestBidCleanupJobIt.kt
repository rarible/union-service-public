package com.rarible.protocol.union.worker.job

import com.rarible.core.test.data.randomString
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.enrichment.repository.ItemRepository
import com.rarible.protocol.union.enrichment.test.data.randomShortItem
import com.rarible.protocol.union.enrichment.test.data.randomShortOrder
import com.rarible.protocol.union.integration.ethereum.data.randomEthItemId
import com.rarible.protocol.union.worker.IntegrationTest
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@IntegrationTest
class BestBidCleanupJobIt {

    @Autowired
    lateinit var itemRepository: ItemRepository

    @Autowired
    lateinit var job: BestBidCleanupJob

    @Test
    fun `execute - ok`() = runBlocking<Unit> {
        val imxItemWithBid = itemRepository.save(
            randomShortItem(randomEthItemId().copy(blockchain = BlockchainDto.IMMUTABLEX)).copy(
                bestBidOrder = randomShortOrder(),
                bestBidOrders = mapOf(randomString() to randomShortOrder())
            )
        )

        val ethItem = itemRepository.save(randomShortItem(randomEthItemId()))
        val imxItem = itemRepository.save(
            randomShortItem(
                randomEthItemId()
                    .copy(blockchain = BlockchainDto.IMMUTABLEX)
            )
        )

        job.reconcile(null, BlockchainDto.IMMUTABLEX).collect()

        val updated = itemRepository.get(imxItemWithBid.id)!!

        assertThat(updated.version).isEqualTo(1)
        assertThat(updated.bestBidOrder).isNull()
        assertThat(updated.bestBidOrders).isEmpty()

        // Nothing else changed
        assertThat(itemRepository.get(imxItem.id)!!.version).isEqualTo(0)
        assertThat(itemRepository.get(ethItem.id)!!.version).isEqualTo(0)
    }
}