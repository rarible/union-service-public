package com.rarible.protocol.union.enrichment.meta

import com.rarible.protocol.union.enrichment.configuration.MetaProperties
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

@Tag("manual")
@Disabled
class ContentMetaServiceTest {

    private val metaProperties = MetaProperties(
        "https://ipfs.rarible.com/",
        10000,
        256 * 1024,
        ""
    )

    private val service: ContentMetaService = ContentMetaService(
        MediaMetaService(metaProperties),
        IpfsUrlResolver(metaProperties),
        MetaProperties("", 1000, 1000, "", false),
        null
    )

    @Test
    fun testVideoWithPreview() = runBlocking {
        val meta = service.getContentMeta("ipfs://ipfs/QmSNhGhcBynr1s9QgPnon8HaiPzE5dKgmqSDNsNXCfDHGs/image.gif")!!
        assertEquals(600, meta.width)
        assertEquals(404, meta.height)
        assertEquals(2559234, meta.size)
    }
}
