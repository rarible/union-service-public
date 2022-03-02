package com.rarible.protocol.union.listener.meta

import com.rarible.core.content.meta.loader.ContentMeta
import com.rarible.core.test.data.randomInt
import com.rarible.core.test.data.randomLong
import com.rarible.core.test.data.randomString
import com.rarible.protocol.union.core.model.UnionImageProperties
import com.rarible.protocol.union.enrichment.meta.CachedContentMetaEntry
import com.rarible.protocol.union.enrichment.meta.UnionContentMetaLoader
import com.rarible.protocol.union.integration.ethereum.data.randomEthItemId
import com.rarible.protocol.union.listener.test.AbstractIntegrationTest
import com.rarible.protocol.union.listener.test.IntegrationTest
import io.mockk.coEvery
import io.mockk.coVerify
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.bson.Document
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

    @Test
    fun `return content meta from cache`() = runBlocking<Unit> {
        val collection = mongoTemplate.getCollection(CachedContentMetaEntry.CACHE_META_COLLECTION).awaitFirst()
        collection.insertOne(
            Document.parse(
                """
                    {
                      "_id": "https://ipfs.rarible.com/ipfs/Qme8u4pEU25CNB1qP7Ag6W9J9VnvmXNsE7nuqQBn7S3CC8/nft.jpg",
                      "data": {
                        "type": "image/jpeg",
                        "width": 3840,
                        "height": 2160,
                        "size": 5246840,
                        "_class": "com.rarible.protocol.union.enrichment.meta.ContentMeta"
                      },
                      "updateDate": "2021-10-14T14:21:04.528Z",
                      "version": 0,
                      "_class": "com.rarible.core.cache.Cache"
                    }
                """.trimIndent()
            )
        ).awaitFirst()

        collection.insertOne(
            Document.parse(
                """
                    {
                      "_id": "https://ipfs.rarible.com//ipfs/QmUj2wgrN6mYiWfgdbp67fUYwgUxYQcHQnxDWwcBEnZTWK/image.jpeg",
                      "data": {
                        "type": "image/jpeg",
                        "width": 1920,
                        "height": 1080,
                        "_class": "com.rarible.protocol.nft.core.model.MediaMeta"
                      },
                      "updateDate": "2021-06-01T12:55:50.545Z",
                      "version": 0,
                      "_class": "ru.roborox.reactive.cache.Cache"
                    }
                """.trimIndent()
            )
        ).awaitFirst()

        assertThat(
            unionContentMetaLoader.fetchContentMeta(
                "https://ipfs.rarible.com/ipfs/Qme8u4pEU25CNB1qP7Ag6W9J9VnvmXNsE7nuqQBn7S3CC8/nft.jpg",
                randomEthItemId()
            )
        ).isEqualTo(
            UnionImageProperties(
                mimeType = "image/jpeg",
                width = 3840,
                height = 2160,
                size = 5246840
            )
        )

        assertThat(
            unionContentMetaLoader.fetchContentMeta(
                "https://ipfs.rarible.com//ipfs/QmUj2wgrN6mYiWfgdbp67fUYwgUxYQcHQnxDWwcBEnZTWK/image.jpeg",
                randomEthItemId()
            )
        ).isEqualTo(
            UnionImageProperties(
                mimeType = "image/jpeg",
                width = 1920,
                height = 1080
            )
        )
        coVerify(exactly = 0) { testContentMetaReceiver.receive(any<String>()) }
    }

    private fun createRandomUrl(): String =
        "https://image.com/${randomString()}"

}
