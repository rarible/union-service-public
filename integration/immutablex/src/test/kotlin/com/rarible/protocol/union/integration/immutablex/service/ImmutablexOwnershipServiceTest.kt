package com.rarible.protocol.union.integration.immutablex.service

import com.rarible.protocol.union.core.model.UnionOwnership
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.BlockchainGroupDto
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.OwnershipIdDto
import com.rarible.protocol.union.dto.UnionAddress
import com.rarible.protocol.union.dto.continuation.page.Page
import com.rarible.protocol.union.integration.immutablex.client.ImmutablexApiClient
import com.rarible.protocol.union.integration.immutablex.dto.ImmutablexAssetsPage
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import java.math.BigInteger

internal class ImmutablexOwnershipServiceTest {

    val client = mockk<ImmutablexApiClient>() {
        coEvery {
            getAsset(ImmutablexData.itemId_1)
        } returns ImmutablexData.item_1

        coEvery {
            getAssetsByCollection(
                ImmutablexData.itemId_1,
                "0x6d13857ca83de08456b2b40aaf09a28e0aab056e",
                isNull(),
                100
            )
        }
    }

    @Test
    fun `should return ownership by id`(): Unit = runBlocking {
        val ownership = ImmutablexOwnershipService(mockk {
            coEvery {
                getAssetsByCollection(
                    ImmutablexData.contract_1,
                    "0x6d13857ca83de08456b2b40aaf09a28e0aab056e",
                    isNull(),
                    100
                )
            } returns ImmutablexAssetsPage("", false, listOf(ImmutablexData.item_1))
        }).getOwnershipById(
            "${ImmutablexData.itemId_1}:0x6d13857ca83de08456b2b40aaf09a28e0aab056e"
        )

        Assertions.assertThat(ownership).isEqualTo(
            UnionOwnership(
                OwnershipIdDto(
                    BlockchainDto.IMMUTABLEX,
                    ImmutablexData.itemId_1,
                    UnionAddress(BlockchainGroupDto.IMMUTABLEX, "0x6d13857ca83de08456b2b40aaf09a28e0aab056e")
                ),
                CollectionIdDto(BlockchainDto.IMMUTABLEX, ImmutablexData.contract_1),
                BigInteger.ONE,
                ImmutablexData.item_1.createdAt!!,
                lazyValue = BigInteger.ZERO
            )
        )
    }

    @Test
    fun `should return ownership by item`(): Unit = runBlocking {
        val ownership = ImmutablexOwnershipService(mockk {
            coEvery {
                getAsset(ImmutablexData.itemId_1)
            } returns ImmutablexData.item_1
        }).getOwnershipsByItem(
            ImmutablexData.itemId_1, null, 100
        )

        Assertions.assertThat(ownership).isEqualTo(
            Page(
                0,
                null,
                listOf(
                    UnionOwnership(
                        OwnershipIdDto(
                            BlockchainDto.IMMUTABLEX,
                            ImmutablexData.itemId_1,
                            UnionAddress(BlockchainGroupDto.IMMUTABLEX, "0x6d13857ca83de08456b2b40aaf09a28e0aab056e")
                        ),
                        CollectionIdDto(BlockchainDto.IMMUTABLEX, ImmutablexData.contract_1),
                        BigInteger.ONE,
                        ImmutablexData.item_1.createdAt!!,
                        lazyValue = BigInteger.ZERO
                    )
                )
            )
        )
    }

}
