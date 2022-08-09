package com.rarible.protocol.union.integration.immutablex.client

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import scalether.domain.Address

class TokenIdDecoderTest {

    @ParameterizedTest
    @ValueSource(
        strings = [
            "0",
            "1",
            "182746287364582376456237452835",
            "3948573874659234652439762437562934854520384578348675349857234985734854938573"
        ]
    )
    fun `valid tokenId`(tokenId: String) {
        val encoded = TokenIdDecoder.encode(tokenId)
        assertThat(encoded.toString()).isEqualTo(tokenId)

        val decoded = TokenIdDecoder.decode(encoded.toString())
        assertThat(decoded).isEqualTo(tokenId)
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "8e842633-fe3d-4e30-a93d-e5c0b0c940ac",
            "some_weird_stuff",
            "a",
            "(*#^$@#_$@#()*%20375"
        ]
    )
    fun `string tokenId`(tokenId: String) {
        val encoded = TokenIdDecoder.encode(tokenId)
        assertThat(encoded.toString()).isNotEqualTo(tokenId)

        val decoded = TokenIdDecoder.decode(encoded.toString())
        assertThat(decoded).isEqualTo(tokenId)
    }

    @Test
    fun `decode itemId`() {
        val tokenId = "abc"
        val encodedTokenId = TokenIdDecoder.encode(tokenId)
        val itemId = "${Address.ZERO()}:${encodedTokenId}"

        val decodedItemId = TokenIdDecoder.decodeItemId(itemId)
        assertThat(decodedItemId).isEqualTo("${Address.ZERO()}:abc")
    }
}