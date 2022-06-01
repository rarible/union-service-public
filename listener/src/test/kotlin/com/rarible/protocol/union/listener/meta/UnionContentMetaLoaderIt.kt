package com.rarible.protocol.union.listener.meta

import com.rarible.core.meta.resource.detector.ContentMeta
import com.rarible.core.test.data.randomInt
import com.rarible.core.test.data.randomLong
import com.rarible.core.test.data.randomString
import com.rarible.protocol.union.core.model.UnionImageProperties
import com.rarible.protocol.union.enrichment.meta.UnionContentMetaLoader
import com.rarible.protocol.union.integration.ethereum.data.randomEthItemId
import com.rarible.protocol.union.listener.test.AbstractIntegrationTest
import com.rarible.protocol.union.listener.test.IntegrationTest
import io.mockk.coEvery
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.ReactiveMongoTemplate

@IntegrationTest
class UnionContentMetaLoaderIt : AbstractIntegrationTest() {

    @Autowired
    private lateinit var unionContentMetaLoader: UnionContentMetaLoader

    @Autowired
    private lateinit var mongoTemplate: ReactiveMongoTemplate

    @Test
    fun `load content meta`() = runBlocking<Unit> {
        val url = createRandomUrl()
        val contentMeta = ContentMeta(
            type = "image/" + randomString(),
            width = randomInt(),
            height = randomInt(),
            size = randomLong()
        )
        coEvery { testContentMetaReceiver.receive(url) } returns contentMeta
        assertThat(unionContentMetaLoader.fetchContentMeta(url, randomEthItemId()))
            .isEqualTo(
                UnionImageProperties(
                    mimeType = contentMeta.type,
                    width = contentMeta.width,
                    height = contentMeta.height,
                    size = contentMeta.size
                )
            )
    }

    private fun createRandomUrl(): String =
        "https://image.com/${randomString()}"

}
