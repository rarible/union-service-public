package com.rarible.protocol.union.integration.immutablex.service

import com.rarible.protocol.union.core.test.ManualTest
import com.rarible.protocol.union.integration.ImxManualTest
import com.rarible.protocol.union.integration.immutablex.client.TokenIdDecoder
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

@ManualTest
class ImxItemServiceMt : ImxManualTest() {

    private val service = ImxItemService(
        assetClient,
        activityClient,
        collectionClient,
        collectionCreatorRepository,
        collectionMetaSchemaRepository
    )

    // Data for test purposes, it is not static and can break these manual tests in any moment
    private val testOwner = "0x38b84a9518a356b85eb29ba76fd78e0d3c718fd8"
    private val testCreator = "0x38b84a9518a356b85eb29ba76fd78e0d3c718fd8"
    private val testCollection = "0x6b11e2eeabfa12ae875ddd9024665b7e7edeac68"
    private val testCollectionUser = "0x47846a7457660f1c585377cd173aa4811580ca31"

    @Test
    fun getRoyalties() = runBlocking<Unit> {
        val result = service.getItemRoyaltiesById("0x6b11e2eeabfa12ae875ddd9024665b7e7edeac68:41")

        println(result)
        assertThat(result).hasSize(2)
    }

    @Test
    fun getMeta() = runBlocking<Unit> {
        val result = service.getItemMetaById("0x311b9817c6eec7fe104d26eae9fbaa003cc12dc8:99999")

        println(result)
        assertThat(result.name).isEqualTo("99999moot")
        assertThat(result.attributes).hasSize(3) // Only 3 filtered attributes of 6 in total should be here
    }

    @Test
    fun getItemById() = runBlocking<Unit> {
        val itemId = "0x5e4b4e5c90f8bd957dfdb79300d9f14bf1a7ec58:1236"
        val result = service.getItemById(itemId)

        println(result)
        assertThat(result.id.value).isEqualTo(itemId)
    }

    @Test
    fun `getItemById - string tokenId`() = runBlocking<Unit> {
        val token = "0x6de6b04d630a4a41bb223815433b9ebf0da50f69"
        val tokenId = "8e842633-fe3d-4e30-a93d-e5c0b0c940ac"
        val tokenIdEncoded = TokenIdDecoder.encode(tokenId)
        val itemId = "${token}:${tokenIdEncoded}"
        val result = service.getItemById(itemId)

        println(result)
        assertThat(result.id.value).isEqualTo(itemId)
    }

    @Test
    fun getItemsByIds() = runBlocking<Unit> {
        val itemIds = listOf(
            "0x5e4b4e5c90f8bd957dfdb79300d9f14bf1a7ec58:1236",
            "0x1ea92417b0393eba0edddea4fb35eb4e12b3165d:5456170", // Doesn't exists
            "0x1ea92417b0393eba0edddea4fb35eb4e12b3165d:544455",
        )
        val result = service.getItemsByIds(itemIds)

        println(result)
        assertThat(result).hasSize(2)
        assertThat(result[0].id.value).isEqualTo(itemIds[0])
        assertThat(result[1].id.value).isEqualTo(itemIds[2])
    }

    @Test
    fun `getItemsByOwner - first page`() = runBlocking<Unit> {
        val result = service.getItemsByOwner(testOwner, null, 50)

        println(result)
        assertThat(result.entities).hasSize(8)
    }

    @Test
    fun `getItemsByOwner - second page`() = runBlocking<Unit> {
        val page1 = service.getItemsByOwner(testOwner, null, 1)

        println(page1)
        assertThat(page1.entities).hasSize(1)
        assertThat(page1.continuation).isNotNull()

        val page2 = service.getItemsByOwner(testOwner, page1.continuation, 10)
        println(page2)

        assertThat(page2.entities).hasSize(7)
        assertThat(page2.continuation).isNull()
    }

    @Test
    fun `getItemsByCreator - first page`() = runBlocking<Unit> {
        val result = service.getItemsByCreator(testCreator, null, 50)
        val itemIds = result.entities.map { it.id.value }.toSet()

        println(result)
        assertThat(itemIds).hasSize(6)
        assertThat(itemIds).contains("0x8670bfbd82f4e33931f1361d7c965dc223fb5f88:1")
    }

    @Test
    fun `getItemsByCreator - second page`() = runBlocking<Unit> {
        val page1 = service.getItemsByCreator(testCreator, null, 1)

        println(page1)
        assertThat(page1.entities).hasSize(1)
        assertThat(page1.continuation).isNotNull()

        val page2 = service.getItemsByCreator(testOwner, page1.continuation, 10)
        println(page2)

        assertThat(page2.entities).hasSize(5)
        assertThat(page2.continuation).isNull()
    }

    @Test
    fun `getItemsByCollection - first page`() = runBlocking<Unit> {
        val result = service.getItemsByCollection(testCollection, testCollectionUser, null, 50)
        val itemIds = result.entities.map { it.id.value }.toSet()

        println(result)
        assertThat(itemIds).hasSize(4)
    }

    @Test
    fun `getItemsByCollection - second page`() = runBlocking<Unit> {
        val page1 = service.getItemsByCollection(testCollection, testCollectionUser, null, 1)

        println(page1)
        assertThat(page1.entities).hasSize(1)
        assertThat(page1.continuation).isNotNull()

        val page2 = service.getItemsByCollection(testCollection, testCollectionUser, page1.continuation, 10)
        println(page2)

        assertThat(page2.entities).hasSize(3)
        assertThat(page2.continuation).isNull()
    }

}