package com.rarible.protocol.union.integration.immutablex.service

import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionDto
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.integration.MockWebClient
import com.rarible.protocol.union.integration.immutablex.client.ImmutablexApiClient
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test


class ImmutablexCollectionServiceTest {

    @Test
    fun `should get all collections`(): Unit = runBlocking {
        val page = ImmutablexCollectionService(
            ImmutablexApiClient(
                MockWebClient(
                    "/collections?order_by=name&direction=desc&page_size=50", ALL_COLLECTIONS
                )
            )
        ).getAllCollections(null, 50)


        Assertions.assertThat(page.entities).containsExactly(
            CollectionDto(
                CollectionIdDto(BlockchainDto.IMMUTABLEX, "0x62d25241d4a5d619c1b06114210250d19d2424c0"),
                null,
                BlockchainDto.IMMUTABLEX,
                CollectionDto.Type.IMMUTABLEX,
                "CERTIFICATE3",
                "CERTIFICATE3",
                null,
                emptyList(),
                null
            ),
            CollectionDto(
                CollectionIdDto(BlockchainDto.IMMUTABLEX, "0xa8106fe2195c3fa48ed52b52f7981f74d18d8ba4"),
                null,
                BlockchainDto.IMMUTABLEX,
                CollectionDto.Type.IMMUTABLEX,
                "CERTIFICATE2",
                "CERTIFICATE2",
                null,
                emptyList(),
                null
            )
        )
    }

    @Test
    fun `should get collection by id`(): Unit = runBlocking {
        val col = ImmutablexCollectionService(
            ImmutablexApiClient(
                MockWebClient(
                    "/collections/0x62d25241d4a5d619c1b06114210250d19d2424c0", ONE_COLLECTION
                )
            )
        ).getCollectionById("0x62d25241d4a5d619c1b06114210250d19d2424c0")

        Assertions.assertThat(col).isEqualTo(
            CollectionDto(
                CollectionIdDto(BlockchainDto.IMMUTABLEX, "0x62d25241d4a5d619c1b06114210250d19d2424c0"),
                null,
                BlockchainDto.IMMUTABLEX,
                CollectionDto.Type.IMMUTABLEX,
                "CERTIFICATE3",
                "CERTIFICATE3",
                null,
                emptyList(),
                null
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