package com.rarible.protocol.union.integration.immutablex.service

import com.rarible.protocol.union.core.test.ManualTest
import com.rarible.protocol.union.integration.ImxManualTest
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

@ManualTest
class ImxOwnershipServiceMt : ImxManualTest() {

    private val service = ImxOwnershipService(
        assetClient,
        ImxItemService(assetClient, activityClient, collectionClient)
    )

    private val itemId = "0x6b11e2eeabfa12ae875ddd9024665b7e7edeac68:30"

    @Test
    fun getOwnershipsById() = runBlocking<Unit> {
        val ownershipId = "$itemId:0xfe1e9caa0baf84f197835a2d139ab307ff119170"
        val result = service.getOwnershipById(ownershipId)
        println(result)
    }

    @Test
    fun getOwnershipsByIds() = runBlocking<Unit> {
        val ownershipIds = listOf(
            "$itemId:0xfe1e9caa0baf84f197835a2d139ab307ff119170", // exists
            "$itemId:0xfe1e9caa0baf84f197835a2d139ab307ff119171"  // doesn't exists
        )
        val result = service.getOwnershipsByIds(ownershipIds)
        println(result)

        assertThat(result).hasSize(1)
        assertThat(result[0].id.value).isEqualTo(ownershipIds[0])
    }

}