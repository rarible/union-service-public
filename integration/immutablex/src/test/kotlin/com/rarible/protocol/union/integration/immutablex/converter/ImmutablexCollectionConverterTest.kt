package com.rarible.protocol.union.integration.immutablex.converter

import com.rarible.protocol.union.dto.CollectionDto
import com.rarible.protocol.union.integration.immutablex.client.ImmutablexCollection
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

class ImmutablexCollectionConverterTest {

    @ParameterizedTest
    @MethodSource("source")
    internal fun `converter test`(source: ImmutablexCollection) {
        val actual = ImmutablexCollectionConverter.convert(source)

        assertThat(actual.id.fullId()).isEqualTo("IMMUTABLEX:${source.address}")
        assertThat(actual.name).isEqualTo(source.name)
        assertThat(actual.symbol).isNull()
        assertThat(actual.owner).isNull()
        assertThat(actual.meta).isNotNull
        assertThat(actual.meta?.name).isEqualTo(source.name)
        assertThat(actual.meta?.description).isEqualTo(source.description)
        if (!source.collectionImageUrl.isNullOrEmpty()) {
            assertThat(actual.meta?.content).isNotEmpty
            assertThat(actual.meta?.content?.get(0)?.url).isEqualTo(source.collectionImageUrl)
        }
        assertThat(actual.features).isEqualTo(listOf(CollectionDto.Features.APPROVE_FOR_ALL))
    }


    companion object {
        @JvmStatic
        fun source(): Stream<Arguments> = Stream.of(
            Arguments.of(
                ImmutablexCollection(
                    address = "0x4620db5d69f1db8b1cfd1902733b32ec7ddcf2fd",
                    name = "SampleNftThree",
                    description = "A Sample Nft",
                    iconUrl = "",
                    collectionImageUrl = "",
                    projectId = 49769L,
                    metadataApiUrl = "https://gateway.pinata.cloud/ipfs/QmNpyHLe9g3A3WuPnRKZjkQ4K6mP5fd5y2LZYPhWksj5SD"
                )
            ),
            Arguments.of(
                ImmutablexCollection(
                    address = "0xfed69f6853e7059212b15ba65ed9e50e2378b6c6",
                    name = "PsuedoMorphs05",
                    description = "lol asdf",
                    iconUrl = null,
                    collectionImageUrl = "https://ipfs.io/ipfs/QmapEdL7C7CUoHURicwACz5wfKBGVDW84ghw7eq7CiJmWL",
                    projectId = 36456L,
                    metadataApiUrl = "https://ipfs.io/ipfs/QmdASYWt2Ho4ixDZiMGbVdgYUsLo7SrTVtJKcLzBRoEcd8"
                )
            ),
            Arguments.of(
                ImmutablexCollection(
                    address = "0x058ed021ce844a91b254432ac4c1423d526ef437",
                    name = "Minting Pass",
                    description = "TODO:",
                    iconUrl = "TODO:",
                    collectionImageUrl = "TODO:",
                    projectId = 50067L,
                    metadataApiUrl = "TODO:/0x058ed021ce844a91b254432ac4c1423d526ef437/"
                )
            ),
            Arguments.of(
                ImmutablexCollection(
                    address = "0xcb08a68c6b9e124357660d9d51b22769df51ffbc",
                    name = "NFT_COLLECTION",
                    description = "NFT_TEST",
                    iconUrl = "https://dev.topfrags.gg/api/thumbnail",
                    collectionImageUrl = "https://dev.topfrags.gg/api/image/thumbnail",
                    projectId = 50265L,
                    metadataApiUrl = "https://dev.topfrags.gg/api/metadata"
                )
            )
        )
    }
}
