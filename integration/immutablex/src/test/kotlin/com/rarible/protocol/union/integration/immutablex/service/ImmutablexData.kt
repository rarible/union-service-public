package com.rarible.protocol.union.integration.immutablex.service

import com.rarible.protocol.union.integration.immutablex.dto.ImmutablexAsset
import com.rarible.protocol.union.integration.immutablex.dto.ImmutablexCollectionShort
import java.time.Instant


object ImmutablexData {
    val contract_1 = "0x62d25241d4a5d619c1b06114210250d19d2424c0"
    val itemId_1 = "0x62d25241d4a5d619c1b06114210250d19d2424c0:1"
    val item_1 = ImmutablexAsset(
        tokenAddress = "0x62d25241d4a5d619c1b06114210250d19d2424c0",
        tokenId = 1,
        id = "0x32c79b61ebc9fba950f84c8f2b5eb870c04c2e442beb48ca164ec463c8fabb8c",
        user = "0x6d13857ca83de08456b2b40aaf09a28e0aab056e",
        status = "imx",
        uri = null,
        name = null,
        description = null,
        imageUrl = null,
        metadata = emptyMap(),
        collection = ImmutablexCollectionShort(
            "CERTIFICATE3",
            "https://lh3.googleusercontent.com/_Wp8FVil4jyOq0hD0eHWXzV4IEhkmrsKv2T4-NvTxih8PUOeGHReu7qmPk0lKajOwAUQV-yxV8qEnxdfWVH1cYgm1KGeMf0bAJYn4A=w600"
        ),
        createdAt = Instant.parse("2022-03-11T07:16:19.105119Z"),
        updatedAt = Instant.parse("2022-03-11T07:16:35.250378Z")
    )
}