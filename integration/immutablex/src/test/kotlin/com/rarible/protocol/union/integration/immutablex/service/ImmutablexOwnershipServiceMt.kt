package com.rarible.protocol.union.integration.immutablex.service

import com.rarible.protocol.union.core.test.ManualTest
import com.rarible.protocol.union.integration.ImmutablexManualTest
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

@ManualTest
class ImmutablexOwnershipServiceMt : ImmutablexManualTest() {

    private val service = ImmutablexOwnershipService(assetClient, activityClient)

    @Test
    fun getOwnershipsByIds() = runBlocking<Unit> {
        val ownershipId = "0x6b11e2eeabfa12ae875ddd9024665b7e7edeac68:30:0xfe1e9caa0baf84f197835a2d139ab307ff119170"
        val result = service.getOwnershipsByIds(listOf(ownershipId))
        println(result)
    }

}