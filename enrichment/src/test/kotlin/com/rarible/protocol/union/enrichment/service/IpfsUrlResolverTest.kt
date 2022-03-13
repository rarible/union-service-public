package com.rarible.protocol.union.enrichment.service

import com.rarible.protocol.union.enrichment.meta.IpfsUrlResolver
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class IpfsUrlResolverTest {
    private val ipfsGateway = "https://rarible.mypinata.cloud"

    private val service = IpfsUrlResolver(
        mockk {
            every { ipfsGateway } returns this@IpfsUrlResolverTest.ipfsGateway
        }
    )

    @Test
    fun testIsCid() {
        assertThat(service.isCid("QmQzqPpcBFkc9AwV4B2tscLy9dBwN7o9yEHE9aRCHeN6KW")).isTrue
        assertThat(service.isCid("QQzqPpcBFkc9AwV4B2tscLy9dBwN7o9yEHE9aRCHeN6KW")).isFalse
        assertThat(service.isCid("bafybeigdyrzt5sfp7udm7hu76uh7y26nf3efuylqabf3oclgtqy55fbzdi")).isTrue
        assertThat(service.isCid("3")).isFalse
        assertThat(service.isCid("f01701220c3c4733ec8affd06cf9e9ff50ffc6bcd2ec85a6170004bb709669c31de94391a")).isTrue
    }

    @Test
    fun testRealUrl() {
        val pairs = listOf(
            "https://ipfs.io/ipfs/https://ipfs.io/ipfs/QmQzqPpcBFkc9AwV4B2tscLy9dBwN7o9yEHE9aRCHeN6KW" to "$ipfsGateway/ipfs/QmQzqPpcBFkc9AwV4B2tscLy9dBwN7o9yEHE9aRCHeN6KW",
            "QmbpJhWFiwzNu7MebvKG3hrYiyWmSiz5dTUYMQLXsjT9vw" to "$ipfsGateway/ipfs/QmbpJhWFiwzNu7MebvKG3hrYiyWmSiz5dTUYMQLXsjT9vw",
            "ipfs://ipfs/QmaMTrfaPkHrD3RsoN7VECBn8Wea6pBg175GCWFNbQRK6R/cusses/some URL with space.gif" to "$ipfsGateway/ipfs/QmaMTrfaPkHrD3RsoN7VECBn8Wea6pBg175GCWFNbQRK6R/cusses/some%20URL%20with%20space.gif",
            "https://api.t-o-s.xyz/ipfs/gucci/8.gif" to "https://api.t-o-s.xyz/ipfs/gucci/8.gif",
            "http://api.guccinfts.xyz/ipfs/8" to "http://api.guccinfts.xyz/ipfs/8"
        )
        for ((input, output) in pairs) {
            assertThat(service.resolveRealUrl(input)).isEqualTo(output)
        }
    }
}
