package com.rarible.protocol.union.integration.immutablex.client

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.web.util.DefaultUriBuilderFactory

class ImxCollectionQueryBuilderTest {

    private val host = "http://imx.test/api"
    private lateinit var builder: ImxCollectionQueryBuilder

    @BeforeEach
    fun beforeEach() {
        builder = ImxCollectionQueryBuilder(DefaultUriBuilderFactory(host).builder())
    }

    @Test
    fun `all - without continuation`() {
        builder.pageSize(10)
        builder.continuationById(null)

        assertThat(builder.toString()).isEqualTo(
            "$host/collections" +
                "?page_size=10" +
                "&order_by=address" +
                "&direction=asc"
        )
    }

    @Test
    fun `with continuation`() {
        val continuation = "0x001061e7b587ee12256e668c90103c09f7773938"
        builder.pageSize(10)
        builder.continuationById(continuation)

        assertThat(builder.toString()).isEqualTo(
            "$host/collections" +
                "?page_size=10" +
                "&cursor=eyJhZGRyZXNzIjoiMHgwMDEwNjFlN2I1ODdlZTEyMjU2ZTY2OGM5MDEwM2MwOWY3NzczOTM4In0" +
                "&order_by=address" +
                "&direction=asc"
        )
    }
}
