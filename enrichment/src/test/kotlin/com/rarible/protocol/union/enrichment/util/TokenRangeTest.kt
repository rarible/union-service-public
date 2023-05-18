package com.rarible.protocol.union.enrichment.util

import com.rarible.core.test.data.randomString
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.enrichment.model.EnrichmentCollectionId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigInteger

class TokenRangeTest {

    private val collectionId = EnrichmentCollectionId(BlockchainDto.ETHEREUM, randomString())
    private val range = TokenRange(collectionId, BigInteger.ONE.rangeTo(BigInteger.TEN))

    @Test
    fun `batch - first`() {
        val batch = range.batch(null, 2)
        assertThat(batch).isEqualTo(listOf(1.toBigInteger(), 2.toBigInteger()))
    }

    @Test
    fun `batch - last`() {
        val batch = range.batch(8.toBigInteger(), 2)
        assertThat(batch).isEqualTo(listOf(9.toBigInteger(), 10.toBigInteger()))
    }

    @Test
    fun `batch - last, oversize`() {
        val batch = range.batch(8.toBigInteger(), 3)
        assertThat(batch).isEqualTo(listOf(9.toBigInteger(), 10.toBigInteger()))
    }

    @Test
    fun `batch - first, oversize`() {
        val batch = range.batch(null, 50)
        assertThat(batch).isEqualTo((1..10).map { it.toBigInteger() })
    }

    @Test
    fun `batch - middle`() {
        val batch = range.batch(5.toBigInteger(), 2)
        assertThat(batch).isEqualTo(listOf(6.toBigInteger(), 7.toBigInteger()))
    }

}