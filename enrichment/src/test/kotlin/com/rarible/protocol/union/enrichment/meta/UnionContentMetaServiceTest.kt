package com.rarible.protocol.union.enrichment.meta

import com.rarible.core.meta.resource.detector.embedded.EmbeddedContentDetector
import com.rarible.core.meta.resource.model.MimeType
import com.rarible.core.meta.resource.parser.UrlParser
import com.rarible.core.meta.resource.resolver.ConstantGatewayProvider
import com.rarible.core.meta.resource.resolver.IpfsGatewayResolver
import com.rarible.core.meta.resource.resolver.LegacyIpfsGatewaySubstitutor
import com.rarible.core.meta.resource.resolver.RandomGatewayProvider
import com.rarible.core.meta.resource.resolver.UrlResolver
import com.rarible.protocol.union.core.model.UnionImageProperties
import com.rarible.protocol.union.enrichment.configuration.EmbeddedContentProperties
import com.rarible.protocol.union.enrichment.meta.embedded.EmbeddedContentUrlProvider
import com.rarible.protocol.union.enrichment.test.data.randomUnionContent
import com.rarible.protocol.union.enrichment.test.data.randomUnionMeta
import com.rarible.protocol.union.integration.ethereum.data.randomEthItemId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

class UnionContentMetaServiceTest {

    private val ipfsGatewayResolver = IpfsGatewayResolver(
        publicGatewayProvider = ConstantGatewayProvider(IPFS_PUBLIC_GATEWAY),
        internalGatewayProvider = RandomGatewayProvider(listOf(IPFS_PRIVATE_GATEWAY)),
        customGatewaysResolver = LegacyIpfsGatewaySubstitutor(listOf(IPFS_CUSTOM_GATEWAY))
    )

    private val metaContentService = UnionContentMetaService(
        UrlParser(),
        UrlResolver(ipfsGatewayResolver),
        EmbeddedContentDetector(),
        EmbeddedContentUrlProvider(EmbeddedContentProperties("http://localhost/media/"))
    )

    @Test
    fun `resolve public ipfs - foreign replaced by public gateway`() {
        // Broken IPFS URL
        assertFixedIpfsUrl("htt://mypinata.com/ipfs/${CID}", CID)
        // Relative IPFS path
        assertFixedIpfsUrl("/ipfs/${CID}/abc.png", "${CID}/abc.png")

        // Abstract IPFS urls with /ipfs/ path and broken slashes
        assertFixedIpfsUrl("ipfs:/ipfs/${CID}", CID)
        assertFixedIpfsUrl("ipfs://ipfs/${CID}", CID)
        assertFixedIpfsUrl("ipfs:///ipfs/${CID}", CID)
        assertFixedIpfsUrl("ipfs:////ipfs/${CID}", CID)

        assertFixedIpfsUrl("ipfs:////ipfs/${CID}", CID)
        assertFixedIpfsUrl("ipfs:////ipfs//${CID}", CID)
        assertFixedIpfsUrl("ipfs:////ipfs///${CID}", CID)
    }

    @Test
    fun `resolve public ipfs - original gateway kept`() {
        // Regular IPFS URL
        assertOriginalIpfsUrl("https://ipfs.io/ipfs/${CID}")
        // Regular IPFS URL with 2 /ipfs/ parts
        assertOriginalIpfsUrl("https://ipfs.io/ipfs/something/ipfs/${CID}")
        // Regular IPFS URL but without CID
        assertOriginalIpfsUrl("http://ipfs.io/ipfs/123.jpg")
    }

    @Test
    fun `resolve public ipfs - prefixed urls`() {
        assertFixedIpfsUrl("ipfs:/folder/${CID}/abc.json", "folder/${CID}/abc.json")
        assertFixedIpfsUrl("ipfs://folder/abc", "folder/abc")
        assertFixedIpfsUrl("ipfs:///folder/subfolder/${CID}", "folder/subfolder/${CID}")
        assertFixedIpfsUrl("ipfs:////${CID}", CID)

        // Various case of ipfs prefix
        assertFixedIpfsUrl("IPFS://${CID}", CID)
        assertFixedIpfsUrl("Ipfs:///${CID}", CID)

        // Abstract IPFS urls with /ipfs/ path and broken slashes without a CID
        assertFixedIpfsUrl("ipfs:/ipfs/abc", "abc")
        assertFixedIpfsUrl("ipfs://ipfs/folder/abc", "folder/abc")
        assertFixedIpfsUrl("ipfs:///ipfs/abc", "abc")
    }

    @Test
    fun `resolve internal ipfs - replaced by internal gateway`() {
        val result = metaContentService.resolveInternalHttpUrl("https://dweb.link/ipfs/${CID}/1.png")
        assertThat(result).isEqualTo("${IPFS_PRIVATE_GATEWAY}/ipfs/${CID}/1.png")
    }

    @Test
    fun `resolve public ipfs - CID`() {
        assertFixedIpfsUrl(CID, CID)
        assertFixedIpfsUrl("$CID/1", "$CID/1")
    }

    @Test
    fun `resolve public ipfs - replace legacy`() {
        assertThat(
            metaContentService.resolveInternalHttpUrl("${IPFS_CUSTOM_GATEWAY}/ipfs/${CID}")
        ).isEqualTo("${IPFS_PRIVATE_GATEWAY}/ipfs/${CID}")
    }

    @Test
    fun `resolve public http`() {
        val https = "https://test.com/8.gif"
        val http = "http://test.com/8.gif"

        assertThat(metaContentService.resolvePublicHttpUrl(http)).isEqualTo(http)
        assertThat(metaContentService.resolvePublicHttpUrl(https)).isEqualTo(https)
    }

    @Test
    fun `resolve internal http`() {
        val https = "https://test.com/8.gif"
        val http = "http://test.com/8.gif"

        assertThat(metaContentService.resolveInternalHttpUrl(http)).isEqualTo(http)
        assertThat(metaContentService.resolveInternalHttpUrl(https)).isEqualTo(https)
    }

    @Test
    fun `detect embedded content - html`() {
        val content = metaContentService.detectEmbeddedContent("<html><svg></svg></html>")!!
        assertThat(content.meta.mimeType).isEqualTo(MimeType.HTML_TEXT.value)
    }

    @Test
    fun `detect embedded content - svg`() {
        val content = metaContentService.detectEmbeddedContent("<svg></svg>")!!
        assertThat(content.meta.mimeType).isEqualTo(MimeType.SVG_XML_IMAGE.value)
    }

    @Test
    fun `detect embedded content - image`() {
        val content = metaContentService.detectEmbeddedContent("data:image/gif;base64,YWJj")!!
        assertThat(content.meta.mimeType).isEqualTo(MimeType.GIF_IMAGE.value)
    }

    @Test
    fun `resolve storage url - ipfs without gateway`() {
        val resource = metaContentService.parseUrl("ipfs:/$CID")!!
        val url = metaContentService.resolveStorageHttpUrl(resource)
        assertThat(url).isEqualTo("ipfs://$CID")
    }

    @Test
    fun `resolve storage url - ipfs with gateway`() {
        val resource = metaContentService.parseUrl("http://ipfs.io/$CID")!!
        val url = metaContentService.resolveStorageHttpUrl(resource)
        assertThat(url).isEqualTo(resource.original)
    }

    @Test
    fun `resolve storage url - schema url`() {
        val resource = metaContentService.parseUrl("ar://abc")!!
        val url = metaContentService.resolveStorageHttpUrl(resource)
        assertThat(url).isEqualTo(resource.original)
    }

    @Test
    fun `resolve storage url - http url`() {
        val resource = metaContentService.parseUrl("http://test.com/abc")!!
        val url = metaContentService.resolveStorageHttpUrl(resource)
        assertThat(url).isEqualTo(resource.original)
    }

    private fun assertFixedIpfsUrl(url: String, expectedPath: String) {
        val result = metaContentService.resolvePublicHttpUrl(url)
        assertThat(result).isEqualTo("${IPFS_PUBLIC_GATEWAY}/ipfs/$expectedPath")
    }

    private fun assertOriginalIpfsUrl(url: String, expectedPath: String? = null) {
        val expected = expectedPath ?: url // in most cases we expect URL not changed
        val result = metaContentService.resolvePublicHttpUrl(url)
        assertThat(result).isEqualTo(expected)
    }

    @ParameterizedTest
    @MethodSource("publicUrls")
    fun `expose public url`(sourceUrl: String, expected: String) {
        val content = randomUnionContent(UnionImageProperties()).copy(url = sourceUrl)

        val meta = randomUnionMeta().copy(content = listOf(content))
        val withPublicUrls = metaContentService.exposePublicUrls(meta, randomEthItemId())!!

        assertThat(withPublicUrls.content[0].url).isEqualTo(expected)
    }

    companion object {

        private const val CID = "QmeqeBpsYTuJL8AZhY9fGBeTj9QuvMVqaZeRWFnjA24QEE"

        const val IPFS_PUBLIC_GATEWAY = "https://ipfs.io"
        const val IPFS_PRIVATE_GATEWAY = "https://ipfs_private.io"
        const val IPFS_CUSTOM_GATEWAY = "https://rarible.mypinata.com" // Legacy

        @JvmStatic
        fun publicUrls(): Stream<Arguments> = Stream.of(
            // Regular HTTP url
            Arguments.of("http://localhost:8080/abc", "http://localhost:8080/abc"),

            // Embedded content URL
            Arguments.of("embedded://$CID", "http://localhost/media/$CID"),

            // Arweave URL
            Arguments.of("ar://test", "https://arweave.net/test"),

            // Full IPFS url with public gateway - should be the same
            Arguments.of(
                "${IPFS_PUBLIC_GATEWAY}/ipfs/$CID", "${IPFS_PUBLIC_GATEWAY}/ipfs/$CID"
            ),
            // Short IPFS url, should be replaced by public gateway
            Arguments.of("ipfs://$CID/a", "${IPFS_PUBLIC_GATEWAY}/ipfs/$CID/a"),
            // IPFS CID, should be replaced by public gateway
            Arguments.of(CID, "${IPFS_PUBLIC_GATEWAY}/ipfs/$CID"),
            // Legacy IPFS URL - should be replaced by current public URL
            Arguments.of("https://rarible.mypinata.com/ipfs/$CID", "${IPFS_PUBLIC_GATEWAY}/ipfs/$CID"),
            // Declared IPFS gateway, should stay the same
            Arguments.of("https://mypinata.com/ipfs/$CID", "https://mypinata.com/ipfs/$CID")
        )
    }

}