package com.rarible.protocol.union.enrichment.meta.simplehash

import com.rarible.protocol.union.enrichment.meta.simplehash.resolver.SimpleHashResolver
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class SimpleHashConverterServiceTest {

    @Test
    fun `should convert nft by default resolver - ok`() {
        val mock: SimpleHashResolver = mockk() {
            every { support(any()) } returns false
        }
        val defaultMock: SimpleHashResolver = mockk() {
            every { support(any()) } returns true
            every { convert(any()) } returns mockk()
        }
        val simpleHashConverterService = object : SimpleHashConverterService() {
            override fun resolvers() = listOf(mock, defaultMock)
        }

        val raw = this::class.java.getResource("/simplehash/nft.json").readText()
        val source = simpleHashConverterService.convertRawToSimpleHashItem(raw)
        val converted = simpleHashConverterService.convert(source)

        Assertions.assertThat(converted).isNotNull
        verify(exactly = 0) { mock.convert(any()) }
        verify(exactly = 1) { defaultMock.convert(any()) }
    }

    @Test
    fun `should convert nft - failed`() {
        val mock: SimpleHashResolver = mockk() {
            every { support(any()) } returns true
        }
        val simpleHashConverterService = object : SimpleHashConverterService() {
            override fun resolvers() = listOf(mock)
        }

        val raw = this::class.java.getResource("/simplehash/nft.json").readText()
        val source = simpleHashConverterService.convertRawToSimpleHashItem(raw)

        assertThrows<RuntimeException> {
            simpleHashConverterService.convert(source)
        }
    }
}