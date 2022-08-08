package com.rarible.protocol.union.integration.immutablex.client

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.web.util.DefaultUriBuilderFactory
import java.time.Instant

class ImmutablexAssetQueryBuilderTest {

    private val host = "http://imx.test/api"
    private lateinit var builder: ImmutablexAssetQueryBuilder

    private val from = Instant.parse("2022-01-01T00:00:00Z")
    private val to = Instant.parse("2022-01-01T06:00:00Z")

    private val continuationDate = Instant.parse("2022-01-01T03:00:00Z")
    private val continuation = "${continuationDate.toEpochMilli()}_1"

    @BeforeEach
    fun beforeEach() {
        builder = ImmutablexAssetQueryBuilder(DefaultUriBuilderFactory(host).builder())
    }

    @Test
    fun `all parameters - without continuation`() {
        builder.owner("o")
        builder.collection("c")
        builder.continuation(from, to, null)

        assertThat(builder.toString()).isEqualTo(
            "$host/assets?include_fees=true" +
                "&user=o" +
                "&collection=c" +
                "&updated_min_timestamp=2022-01-01T00:00:00Z" +
                "&updated_max_timestamp=2022-01-01T06:00:00Z" +
                "&order_by=updated_at" +
                "&direction=desc"
        )
    }

    @Test
    fun `without date parameters - with continuation`() {
        builder.continuation(null, null, continuation)

        assertThat(builder.toString()).isEqualTo(
            "$host/assets?include_fees=true" +
                "&updated_max_timestamp=2022-01-01T03:00:00Z" +
                "&order_by=updated_at" +
                "&direction=desc"
        )
    }

    @Test
    fun `from - with continuation, desc`() {
        builder.continuation(from, null, continuation)

        // max should be added, min should be the same
        assertThat(builder.toString()).isEqualTo(
            "$host/assets?include_fees=true" +
                "&updated_min_timestamp=2022-01-01T00:00:00Z" +
                "&updated_max_timestamp=2022-01-01T03:00:00Z" +
                "&order_by=updated_at" +
                "&direction=desc"
        )
    }

    @Test
    fun `to - with continuation, desc`() {
        builder.continuation(null, to, continuation)

        // max should be replaced by date from continuation
        assertThat(builder.toString()).isEqualTo(
            "$host/assets?include_fees=true" +
                "&updated_max_timestamp=2022-01-01T03:00:00Z" +
                "&order_by=updated_at" +
                "&direction=desc"
        )
    }
}