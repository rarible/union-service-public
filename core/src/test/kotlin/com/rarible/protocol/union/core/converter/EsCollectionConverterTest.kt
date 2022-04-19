package com.rarible.protocol.union.core.converter

import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomString
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionDto
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.CollectionMetaDto
import com.rarible.protocol.union.dto.UnionAddress
import com.rarible.protocol.union.dto.group
import java.util.stream.Stream
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

class EsCollectionConverterTest {


    @ParameterizedTest
    @MethodSource("args")
    internal fun `collection parser test`(unionCollection: CollectionDto) {
        val esCollection = EsCollectionConverter.convert(unionCollection)
        assertThat(esCollection.collectionId).isEqualTo(unionCollection.id.fullId())
        assertThat(esCollection.name).isEqualTo(unionCollection.name)
        assertThat(esCollection.type).isEqualTo(unionCollection.type.name)
        assertThat(esCollection.symbol).isEqualTo(unionCollection.symbol)
        assertThat(esCollection.owner).isEqualTo(unionCollection.owner?.fullId())
        assertThat(esCollection.parent).isEqualTo(unionCollection.parent?.fullId())
        unionCollection.meta?.let {
            assertThat(esCollection.meta).isNotNull
            assertThat(esCollection.meta!!.name).isEqualTo(it.name)
            assertThat(esCollection.meta!!.description).isEqualTo(it.description)
            assertThat(esCollection.meta!!.feeRecipient).isEqualTo(it.feeRecipient?.fullId())
        }
    }

    companion object {
        @JvmStatic
        private fun args(): Stream<Arguments> {
            return listOf(
                CollectionDto(
                    id = CollectionIdDto(BlockchainDto.ETHEREUM, randomAddress().toString()),
                    blockchain = BlockchainDto.ETHEREUM,
                    name = randomString(),
                    type = CollectionDto.Type.ERC721,
                    minters = listOf(
                        UnionAddress(BlockchainDto.ETHEREUM.group(), randomAddress().toString())
                    ),
                    features = listOf(CollectionDto.Features.BURN),
                    meta = CollectionMetaDto(
                        name = randomString(),
                        description = randomString(),
                        feeRecipient = UnionAddress(BlockchainDto.ETHEREUM.group(), randomAddress().toString())
                    ),
                    owner = UnionAddress(BlockchainDto.ETHEREUM.group(), randomAddress().toString())
                ),
                CollectionDto(
                    id = CollectionIdDto(BlockchainDto.FLOW, randomString()),
                    blockchain = BlockchainDto.FLOW,
                    name = randomString(),
                    type = CollectionDto.Type.FLOW,
                    features = listOf(CollectionDto.Features.BURN),
                    meta = CollectionMetaDto(
                        name = randomString(),
                    ),
                    symbol = randomString(),
                    owner = UnionAddress(BlockchainDto.FLOW.group(), randomString()),
                    parent = CollectionIdDto(BlockchainDto.FLOW, randomString())
                ),
                CollectionDto(
                    id = CollectionIdDto(BlockchainDto.SOLANA, randomString()),
                    blockchain = BlockchainDto.SOLANA,
                    name = randomString(),
                    type = CollectionDto.Type.SOLANA,
                    features = listOf(CollectionDto.Features.BURN),
                    meta = null
                ),
            ).map {
                Arguments.of(it)
            }.stream()
        }

    }
}
