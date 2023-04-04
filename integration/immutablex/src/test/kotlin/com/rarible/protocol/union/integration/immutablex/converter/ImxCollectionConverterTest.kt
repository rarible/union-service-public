package com.rarible.protocol.union.integration.immutablex.converter

import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomString
import com.rarible.protocol.union.core.model.UnionCollection
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.integration.data.randomImxCollection
import com.rarible.protocol.union.integration.immutablex.client.ImmutablexCollection
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ImxCollectionConverterTest {

    private val blockchain = BlockchainDto.IMMUTABLEX

    @Test
    fun `convert collection`() {
        val imxCollection = randomImxCollection()
        val collection = ImxCollectionConverter.convert(imxCollection, blockchain)

        assertThat(collection.id.fullId()).isEqualTo("IMMUTABLEX:${imxCollection.address}")
        assertThat(collection.name).isEqualTo(imxCollection.name)
        assertThat(collection.symbol).isNull()
        assertThat(collection.owner!!.value).isEqualTo(imxCollection.projectOwnerAddress)

        val meta = collection.meta!!
        assertThat(meta.name).isEqualTo(imxCollection.name)
        assertThat(meta.description).isEqualTo(imxCollection.description)

        assertThat(meta.content).hasSize(1)
        assertThat(meta.content[0].url).isEqualTo(imxCollection.collectionImageUrl)

        assertThat(collection.features).isEqualTo(listOf(UnionCollection.Features.APPROVE_FOR_ALL))

        assertThat(collection.minters).hasSize(1)
        assertThat(collection.minters!![0].value).isEqualTo(imxCollection.projectOwnerAddress)

        assertThat(collection.owner!!.value).isEqualTo(imxCollection.projectOwnerAddress)
    }

    @Test
    fun `convert collection - non-required fields are missing`() {
        val imxCollection = ImmutablexCollection(
            address = randomAddress().prefixed(),
            name = randomString(),
            projectId = null,
            description = null,
            iconUrl = null,
            collectionImageUrl = null,
            projectOwnerAddress = null,
            metadataApiUrl = null,
            createdAt = null,
            updatedAt = null
        )

        val collection = ImxCollectionConverter.convert(imxCollection, blockchain)

        assertThat(collection.name).isEqualTo(imxCollection.name)
        assertThat(collection.owner).isNull()

        val meta = collection.meta!!
        assertThat(meta.name).isEqualTo(imxCollection.name)
        assertThat(meta.description).isNull()
        assertThat(meta.content).isEmpty()

        assertThat(collection.features).isEqualTo(listOf(UnionCollection.Features.APPROVE_FOR_ALL))
    }
}
