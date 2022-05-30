package com.rarible.protocol.union.enrichment.meta

import com.rarible.loader.cache.CacheLoaderService
import com.rarible.protocol.union.core.model.UnionImageProperties
import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.enrichment.ResourceTestData.CID
import com.rarible.protocol.union.enrichment.ResourceTestData.IPFS_PUBLIC_GATEWAY
import com.rarible.protocol.union.enrichment.ResourceTestData.ipfsUrlResolver
import com.rarible.protocol.union.enrichment.configuration.UnionMetaProperties
import com.rarible.protocol.union.enrichment.test.data.randomUnionContent
import com.rarible.protocol.union.enrichment.test.data.randomUnionMeta
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class UnionMetaServiceTest {

    private val unionMetaCacheLoaderService: CacheLoaderService<UnionMeta> = mockk()
    private val unionMetaMetrics: UnionMetaMetrics = mockk()
    private val unionMetaLoader: UnionMetaLoader = mockk()

    private val unionMetaService = UnionMetaService(
        unionMetaCacheLoaderService,
        unionMetaMetrics,
        unionMetaLoader,
        ipfsUrlResolver
    )

    @Test
    fun `expose item urls`() {
        // Should be replaced by public IPFS (no matter what CID here)
        val content1 = randomUnionContent(UnionImageProperties()).copy(url = "https://rarible.mypinata.com/ipfs/$CID")
        // Should stay the same
        val content2 = randomUnionContent(UnionImageProperties()).copy(url = "$IPFS_PUBLIC_GATEWAY/ipfs/$CID")
        // Not a legacy ipfs - should be kept as is
        val content3 = randomUnionContent(UnionImageProperties()).copy(url = "https://mypinata.com/ipfs/$CID")

        val meta = randomUnionMeta().copy(content = listOf(content1, content2, content3))

        val withPublicIpfs = unionMetaService.exposePublicIpfsUrls(meta)!!

        assertThat(withPublicIpfs.content[0].url).isEqualTo("$IPFS_PUBLIC_GATEWAY/ipfs/$CID")
        assertThat(withPublicIpfs.content[1].url).isEqualTo("$IPFS_PUBLIC_GATEWAY/ipfs/$CID")
        assertThat(withPublicIpfs.content[2].url).isEqualTo("https://mypinata.com/ipfs/$CID")

    }
}
