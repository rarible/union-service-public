package com.rarible.protocol.union.enrichment.repository.search.internal

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

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
    fun `should fix legacy cursor without second part`() {
        // when
        val actual = service.tryFixLegacyCursor("POLYGON:1657495888423")

        // then
        assertThat(actual).isEqualTo("1657495888423_A")
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
        val actual = service.buildSearchAfterClause("123_456", 2)
        assertThat(actual).containsExactly("123", "456")
    }

    @ParameterizedTest
    @ValueSource(strings = ["", "null", "undefined"])
    fun `should build search after, special words as null`(cursor: String) {
        val actual = service.buildSearchAfterClause(cursor, 1)
        assertThat(actual).isNull()
    }

    @ParameterizedTest
    @ValueSource(strings = ["123", "123_456_789", "123_456_789_1011"])
    fun `should build search after, incorrect size as null`(cursor: String) {
        val actual = service.buildSearchAfterClause(cursor, 2)
        assertThat(actual).isNull()
    }
}
