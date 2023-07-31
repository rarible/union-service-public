package com.rarible.protocol.union.core.util

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigInteger

class CompositeItemIdParserTest {

    @Test
    fun `should split itemId with 3 parts`() {
        // given
        val expected = "ETHEREUM:0x1234" to BigInteger.valueOf(5678)

        // when
        val actual = CompositeItemIdParser.splitWithBlockchain("ETHEREUM:0x1234:5678")

        // then
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `should split itemId with 2 parts (no tokenId)`() {
        // given
        val expected = "SOLANA:0x1234" to null

        // when
        val actual = CompositeItemIdParser.splitWithBlockchain("SOLANA:0x1234")

        // then
        assertThat(actual).isEqualTo(expected)
    }
}
