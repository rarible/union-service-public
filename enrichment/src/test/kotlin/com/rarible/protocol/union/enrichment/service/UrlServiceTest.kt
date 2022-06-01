package com.rarible.protocol.union.enrichment.service

import com.rarible.protocol.union.enrichment.ResourceTestData.CID
import com.rarible.protocol.union.enrichment.ResourceTestData.IPFS_CUSTOM_GATEWAY
import com.rarible.protocol.union.enrichment.ResourceTestData.IPFS_PRIVATE_GATEWAY
import com.rarible.protocol.union.enrichment.ResourceTestData.IPFS_PUBLIC_GATEWAY
import com.rarible.protocol.union.enrichment.ResourceTestData.ITEM_ID
import com.rarible.protocol.union.enrichment.ResourceTestData.urlService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class UrlServiceTest {

    @Test
    fun `foreign ipfs urls - replaced by public gateway`() {
        // Broken IPFS URL
        assertFixedIpfsUrl("htt://mypinata.com/ipfs/$CID", CID)
        // Relative IPFS path
        assertFixedIpfsUrl("/ipfs/$CID/abc.png", "$CID/abc.png")

        // Abstract IPFS urls with /ipfs/ path and broken slashes
        assertFixedIpfsUrl("ipfs:/ipfs/$CID", CID)
        assertFixedIpfsUrl("ipfs://ipfs/$CID", CID)
        assertFixedIpfsUrl("ipfs:///ipfs/$CID", CID)
        assertFixedIpfsUrl("ipfs:////ipfs/$CID", CID)

        assertFixedIpfsUrl("ipfs:////ipfs/$CID", CID)
        assertFixedIpfsUrl("ipfs:////ipfs//$CID", CID)
        assertFixedIpfsUrl("ipfs:////ipfs///$CID", CID)
    }

    @Test
    fun `foreign ipfs urls - original gateway kept`() {
        // Regular IPFS URL
        assertOriginalIpfsUrl("https://ipfs.io/ipfs/$CID")
        // Regular IPFS URL with 2 /ipfs/ parts
        assertOriginalIpfsUrl("https://ipfs.io/ipfs/something/ipfs/$CID")
        // Regular IPFS URL but without CID
        assertOriginalIpfsUrl("http://ipfs.io/ipfs/123.jpg")
    }

    @Test
    fun `prefixed ipfs urls`() {
        assertFixedIpfsUrl("ipfs:/folder/$CID/abc.json", "folder/$CID/abc.json")
        assertFixedIpfsUrl("ipfs://folder/abc", "folder/abc")
        assertFixedIpfsUrl("ipfs:///folder/subfolder/$CID", "folder/subfolder/$CID")
        assertFixedIpfsUrl("ipfs:////$CID", CID)

        // Various case of ipfs prefix
        assertFixedIpfsUrl("IPFS://$CID", CID)
        assertFixedIpfsUrl("Ipfs:///$CID", CID)

        // Abstract IPFS urls with /ipfs/ path and broken slashes without a CID
        assertFixedIpfsUrl("ipfs:/ipfs/abc", "abc")
        assertFixedIpfsUrl("ipfs://ipfs/folder/abc", "folder/abc")
        assertFixedIpfsUrl("ipfs:///ipfs/abc", "abc")
    }

    @Test
    fun `foreign ipfs urls - replaced by internal gateway`() {
        val result = urlService.resolveInnerHttpUrl("https://dweb.link/ipfs/$CID/1.png", ITEM_ID)
        assertThat(result).isEqualTo("${IPFS_PRIVATE_GATEWAY}/ipfs/$CID/1.png")
    }

    @Test
    fun `single sid`() {
        assertFixedIpfsUrl(CID, CID)
    }

    @Test
    fun `regular url`() {
        val https = "https://api.t-o-s.xyz/ipfs/gucci/8.gif"
        val http = "http://api.guccinfts.xyz/ipfs/8"

        assertThat(urlService.resolvePublicHttpUrl(http, ITEM_ID)).isEqualTo(http)
        assertThat(urlService.resolvePublicHttpUrl(https, ITEM_ID)).isEqualTo(https)
    }

    @Test
    fun `replace legacy`() {
        assertThat(urlService.resolveInnerHttpUrl("$IPFS_CUSTOM_GATEWAY/ipfs/$CID", ITEM_ID)).isEqualTo("$IPFS_PRIVATE_GATEWAY/ipfs/$CID")
    }

    private fun assertFixedIpfsUrl(url: String, expectedPath: String) {
        val result = urlService.resolvePublicHttpUrl(url, ITEM_ID)
        assertThat(result).isEqualTo("${IPFS_PUBLIC_GATEWAY}/ipfs/$expectedPath")
    }

    private fun assertOriginalIpfsUrl(url: String, expectedPath: String? = null) {
        val expected = expectedPath ?: url // in most cases we expect URL not changed
        val result = urlService.resolvePublicHttpUrl(url, ITEM_ID)
        assertThat(result).isEqualTo(expected)
    }
}
