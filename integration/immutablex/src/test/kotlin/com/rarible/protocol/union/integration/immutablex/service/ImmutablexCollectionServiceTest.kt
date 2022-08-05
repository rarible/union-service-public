package com.rarible.protocol.union.integration.immutablex.service

import com.rarible.protocol.union.core.model.UnionCollection
import com.rarible.protocol.union.core.model.UnionCollectionMeta
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionDto
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.integration.MockWebClient
import com.rarible.protocol.union.integration.immutablex.client.ImmutablexCollectionClient
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

class ImmutablexCollectionServiceTest {

    @Test
    fun `should get all collections`(): Unit = runBlocking {
        val page = ImmutablexCollectionService(
            ImmutablexCollectionClient(
                MockWebClient(
                    "/collections?order_by=address&direction=asc&page_size=50", ALL_COLLECTIONS
                )
            )
        ).getAllCollections(null, 50)

        // TODO there is no point to check entire object, it should be covered in converter tests
        Assertions.assertThat(page.entities).containsExactly(
            UnionCollection(
                id = CollectionIdDto(BlockchainDto.IMMUTABLEX, "0x62d25241d4a5d619c1b06114210250d19d2424c0"),
                parent = null,
                type = CollectionDto.Type.ERC721,
                name = "CERTIFICATE3",
                symbol = null,
                owner = null,
                features = listOf(CollectionDto.Features.APPROVE_FOR_ALL),
                minters = emptyList(),
                meta = UnionCollectionMeta(
                    name = "CERTIFICATE3",
                    description = "ASRA City DAO is an innovative, utility focused NFT project- decentralizing the collective ownership, development, and governance of virtual land and experiences in the Metaverse.",
                    content = emptyList()
                )
            ),
            UnionCollection(
                id = CollectionIdDto(BlockchainDto.IMMUTABLEX, "0xa8106fe2195c3fa48ed52b52f7981f74d18d8ba4"),
                parent = null,
                type = CollectionDto.Type.ERC721,
                name = "CERTIFICATE2",
                symbol = null,
                owner = null,
                features = listOf(CollectionDto.Features.APPROVE_FOR_ALL),
                minters = emptyList(),
                meta = UnionCollectionMeta(
                    name = "CERTIFICATE2",
                    description = "",
                    content = emptyList()
                )
            )
        )
    }

    @Test
    fun `should get collection by id`(): Unit = runBlocking {
        val col = ImmutablexCollectionService(
            ImmutablexCollectionClient(
                MockWebClient(
                    "/collections/0x62d25241d4a5d619c1b06114210250d19d2424c0", ONE_COLLECTION
                )
            )
        ).getCollectionById("0x62d25241d4a5d619c1b06114210250d19d2424c0")

        // TODO there is no point to check entire object, it should be covered in converter tests
        Assertions.assertThat(col).isEqualTo(
            UnionCollection(
                id = CollectionIdDto(BlockchainDto.IMMUTABLEX, "0x62d25241d4a5d619c1b06114210250d19d2424c0"),
                parent = null,
                type = CollectionDto.Type.ERC721,
                name = "CERTIFICATE3",
                symbol = null,
                minters = emptyList(),
                features = listOf(CollectionDto.Features.APPROVE_FOR_ALL),
                meta = UnionCollectionMeta(
                    name = "CERTIFICATE3",
                    description = "ASRA City DAO is an innovative, utility focused NFT project- decentralizing the collective ownership, development, and governance of virtual land and experiences in the Metaverse.",
                    content = emptyList()
                )
            )
        )
    }

    companion object {

        const val ALL_COLLECTIONS = """
            {
              "result": [
                {
                  "address": "0x62d25241d4a5d619c1b06114210250d19d2424c0",
                  "name": "CERTIFICATE3",
                  "description": "ASRA City DAO is an innovative, utility focused NFT project- decentralizing the collective ownership, development, and governance of virtual land and experiences in the Metaverse.",
                  "icon_url": "https://lh3.googleusercontent.com/_Wp8FVil4jyOq0hD0eHWXzV4IEhkmrsKv2T4-NvTxih8PUOeGHReu7qmPk0lKajOwAUQV-yxV8qEnxdfWVH1cYgm1KGeMf0bAJYn4A=w600",
                  "collection_image_url": "",
                  "project_id": 23982,
                  "metadata_api_url": ""
                },
                {
                  "address": "0xa8106fe2195c3fa48ed52b52f7981f74d18d8ba4",
                  "name": "CERTIFICATE2",
                  "description": "",
                  "icon_url": "",
                  "collection_image_url": "",
                  "project_id": 23916,
                  "metadata_api_url": ""
                }
              ],
              "cursor": "eyJhZGRyZXNzIjoiMHgwMDhiOTI5YWVlZDhiNzY1NTA5YzIwN2VjN2UwOTk1MzI2N2JlNDczIiwibmFtZSI6IklNWCBTRUNSRVQiLCJjcmVhdGVkX2F0IjoiMjAyMi0wMy0wM1QxNTo1Mzo0Mi4wOTU3MDZaIiwidXBkYXRlZF9hdCI6IjIwMjItMDMtMDNUMTU6NTM6NTQuMzI2MzdaIiwiQ29sbGVjdGlvbkltYWdlVXJsIjoiIiwicHJvamVjdF9pZCI6MjA2MTMsIm1ldGFkYXRhX2FwaV91cmwiOiJodHRwczovL2Nkbi5pbXhyYXJpdHkuaW8vZGVtby9tZXRhZGF0YSJ9",
              "remaining": 1
            }
        """

        const val ONE_COLLECTION = """
            {
              "address": "0x62d25241d4a5d619c1b06114210250d19d2424c0",
              "name": "CERTIFICATE3",
              "description": "ASRA City DAO is an innovative, utility focused NFT project- decentralizing the collective ownership, development, and governance of virtual land and experiences in the Metaverse.",
              "icon_url": "https://lh3.googleusercontent.com/_Wp8FVil4jyOq0hD0eHWXzV4IEhkmrsKv2T4-NvTxih8PUOeGHReu7qmPk0lKajOwAUQV-yxV8qEnxdfWVH1cYgm1KGeMf0bAJYn4A=w600",
              "collection_image_url": "",
              "project_id": 23982,
              "metadata_api_url": ""
            }
        """
    }
}
