package com.rarible.protocol.union.integration.immutablex.client

import com.rarible.protocol.union.dto.ActivitySortDto
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.web.util.DefaultUriBuilderFactory
import java.time.Instant

class ImmutablexActivityQueryBuilderTest {

    private val host = "http://imx.test/api"
    private lateinit var builder: MintQueryBuilder

    private val from = Instant.parse("2022-01-01T00:00:00Z")
    private val to = Instant.parse("2022-01-01T06:00:00Z")

    private val continuationDate = Instant.parse("2022-01-01T03:00:00Z")
    private val continuation = "${continuationDate.toEpochMilli()}_1"

    @BeforeEach
    fun beforeEach() {
        builder = MintQueryBuilder(DefaultUriBuilderFactory(host).builder())
    }

    @Test
    fun `all parameters - without continuation`() {
        builder.user("u")
        builder.token("a")
        builder.tokenId("b")
        builder.continuation(from, to, ActivitySortDto.EARLIEST_FIRST, null)

        assertThat(builder.toString()).isEqualTo(
            "$host/mints?" +
                "user=u" +
                "&token_address=a" +
                "&token_id=b" +
                "&min_timestamp=2022-01-01T00:00:00Z" +
                "&max_timestamp=2022-01-01T06:00:00Z" +
                "&order_by=created_at" +
                "&direction=asc"
        )
    }

    @Test
    fun `without date parameters - with continuation, asc`() {
        builder.continuation(null, null, ActivitySortDto.EARLIEST_FIRST, continuation)

        assertThat(builder.toString()).isEqualTo(
            "$host/mints?" +
                "min_timestamp=2022-01-01T03:00:00Z" +
                "&order_by=created_at" +
                "&direction=asc"
        )
    }

    @Test
    fun `without date parameters - with continuation, desc`() {
        builder.continuation(null, null, ActivitySortDto.LATEST_FIRST, continuation)

        assertThat(builder.toString()).isEqualTo(
            "$host/mints?" +
                "max_timestamp=2022-01-01T03:00:00Z" +
                "&order_by=created_at" +
                "&direction=desc"
        )
    }

    @Test
    fun `from - with continuation, asc`() {
        builder.continuation(from, null, ActivitySortDto.EARLIEST_FIRST, continuation)

        // min should be replaced by date from continuation
        assertThat(builder.toString()).isEqualTo(
            "$host/mints?" +
                "min_timestamp=2022-01-01T03:00:00Z" +
                "&order_by=created_at" +
                "&direction=asc"
        )
    }

    @Test
    fun `to - with continuation, asc`() {
        builder.continuation(null, to, ActivitySortDto.EARLIEST_FIRST, continuation)

        // min should be added, max should be the same
        assertThat(builder.toString()).isEqualTo(
            "$host/mints?" +
                "min_timestamp=2022-01-01T03:00:00Z" +
                "&max_timestamp=2022-01-01T06:00:00Z" +
                "&order_by=created_at" +
                "&direction=asc"
        )
    }

    @Test
    fun `from - with continuation, desc`() {
        builder.continuation(from, null, ActivitySortDto.LATEST_FIRST, continuation)

        // max should be added, min should be the same
        assertThat(builder.toString()).isEqualTo(
            "$host/mints?" +
                "min_timestamp=2022-01-01T00:00:00Z" +
                "&max_timestamp=2022-01-01T03:00:00Z" +
                "&order_by=created_at" +
                "&direction=desc"
        )
    }

    @Test
    fun `to - with continuation, desc`() {
        builder.continuation(null, to, ActivitySortDto.LATEST_FIRST, continuation)

        // max should be replaced by date from continuation
        assertThat(builder.toString()).isEqualTo(
            "$host/mints?" +
                "max_timestamp=2022-01-01T03:00:00Z" +
                "&order_by=created_at" +
                "&direction=desc"
        )
    }

}