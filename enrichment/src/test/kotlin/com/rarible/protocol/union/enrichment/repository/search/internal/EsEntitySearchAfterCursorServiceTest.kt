package com.rarible.protocol.union.enrichment.repository.search.internal

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class EsEntitySearchAfterCursorServiceTest {

    private val service = EsEntitySearchAfterCursorService()

    @Test
    fun `should fix legacy cursor`() {
        // when
        val actual = service.tryFixLegacyCursor("POLYGON:1657495888423_4556657")

        // then
        assertThat(actual).isEqualTo("1657495888423_4556657")
    }

    @Test
    fun `should not fix cursor`() {
        // when
        val actual = service.tryFixLegacyCursor("1657495888423_123456")

        // then
       assertThat(actual).isEqualTo("1657495888423_123456")
    }

    @Test
    fun `should build search after clause`() {
        val actual = service.buildSearchAfterClause("123_456")
        assertThat(actual).containsExactly("123", "456")
    }
}
