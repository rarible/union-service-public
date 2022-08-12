package com.rarible.protocol.union.integration.immutablex.client

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.web.util.DefaultUriBuilderFactory
import java.time.Instant

class ImxAssetQueryBuilderTest {

    private val host = "http://imx.test/api"
    private lateinit var builder: ImxAssetQueryBuilder

    private val from = Instant.parse("2022-01-01T00:00:00Z")
    private val to = Instant.parse("2022-01-01T06:00:00Z")

    private val continuationDate = Instant.parse("2022-01-01T03:00:00Z")
    private val continuation = "${continuationDate.toEpochMilli()}_abc:1"

    @BeforeEach
    fun beforeEach() {
        builder = ImxAssetQueryBuilder(DefaultUriBuilderFactory(host).builder())
    }

    @Test
    fun `all - without continuation`() {
        builder.owner("o")
        builder.collection("c")
        builder.fromDate(from)
        builder.toDate(to)
        builder.continuation(
            null, false
        )
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
    fun `with continuation`() {
        builder.continuation(continuation, false)

        assertThat(builder.toString()).isEqualTo(
            "$host/assets?include_fees=true" +
                "&cursor=eyJjb250cmFjdF9hZGRyZXNzIjoiYWJjIiwiY2xpZW50X3Rva2VuX2lkIjoiMSIsInVwZGF0ZWRfYXQiOiIyMDIyLTAxLTAxVDAyOjU5OjU5Ljk5OVoifQ" +
                "&order_by=updated_at" +
                "&direction=desc"
        )
    }
}