package com.rarible.protocol.union.integration.immutablex.converter

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.rarible.protocol.union.core.model.itemId
import com.rarible.protocol.union.integration.immutablex.client.ImmutablexMint
import com.rarible.protocol.union.integration.immutablex.client.ImmutablexTokenEvent
import com.rarible.protocol.union.integration.immutablex.client.ImmutablexTransfer
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

class ImmutablexActivityConverterTest {

    private val converter = ImmutablexEventConverter(mockk())

    @ParameterizedTest
    @MethodSource("data")
    internal fun `should convert simple token event`(source: ImmutablexTokenEvent) {
        runBlocking {
            val activity = converter.convert(source)
            assertThat(activity).isNotNull
            assertThat(activity.id).isEqualTo(source.activityId)
            assertThat(activity.date).isEqualTo(source.timestamp)
            assertThat(activity.lastUpdatedAt).isNull()
            assertThat("${activity.itemId()}").isEqualTo("IMMUTABLEX:${source.itemId()}")
        }
    }

    companion object {

        @JvmStatic
        fun data(): Stream<Arguments> {
            val mapper = jacksonObjectMapper().registerModule(JavaTimeModule())
            val resourcePath = "/com/rarible/protocol/union/integration/immutablex/service/"
            return Stream.of(
                Arguments.of(
                    ImmutablexActivityConverterTest::class.java.getResourceAsStream("${resourcePath}mint.json").use {
                        mapper.readValue(it!!, ImmutablexMint::class.java)
                    }
                ),
                Arguments.of(
                    ImmutablexActivityConverterTest::class.java.getResourceAsStream("${resourcePath}transfer.json").use {
                        mapper.readValue(it!!, ImmutablexTransfer::class.java)
                    }
                )
            )
        }
    }
}
