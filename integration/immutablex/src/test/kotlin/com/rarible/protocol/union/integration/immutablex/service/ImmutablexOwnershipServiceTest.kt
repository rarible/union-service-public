package com.rarible.protocol.union.integration.immutablex.service

import com.rarible.protocol.union.core.model.UnionOwnership
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.BlockchainGroupDto
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.CreatorDto
import com.rarible.protocol.union.dto.OwnershipIdDto
import com.rarible.protocol.union.dto.UnionAddress
import com.rarible.protocol.union.dto.continuation.page.Page
import com.rarible.protocol.union.dto.group
import com.rarible.protocol.union.integration.immutablex.dto.ImmutablexAssetsPage
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import java.math.BigInteger

internal class ImmutablexOwnershipServiceTest {

    @Test
    fun `should return ownership by id`(): Unit = runBlocking {
        val ownership = ImmutablexOwnershipService(
            mockk {
                coEvery {
                    getAssetsByCollection(
                        ImmutablexData.contract_1,
                        "0x6d13857ca83de08456b2b40aaf09a28e0aab056e",
                        isNull(),
                        100
                    )
                } returns ImmutablexAssetsPage("", false, listOf(ImmutablexData.item_1))
            },

            mockk {
                coEvery {
                    getItemCreator(any())
                } returns "0x6d13857ca83de08456b2b40aaf09a28e0aab056e"
            })
            .getOwnershipById("${ImmutablexData.itemId_1}:0x6d13857ca83de08456b2b40aaf09a28e0aab056e")

        Assertions.assertThat(ownership).isEqualTo(
            UnionOwnership(
                OwnershipIdDto(
                    BlockchainDto.IMMUTABLEX,
                    ImmutablexData.itemId_1,
                    UnionAddress(BlockchainGroupDto.ETHEREUM, "0x6d13857ca83de08456b2b40aaf09a28e0aab056e")
                ),
                CollectionIdDto(BlockchainDto.IMMUTABLEX, ImmutablexData.contract_1),
                BigInteger.ONE,
                ImmutablexData.item_1.createdAt!!,
                lazyValue = BigInteger.ZERO,
                lastUpdatedAt = ImmutablexData.item_1.updatedAt!!,
                creators = listOf(
                    CreatorDto(
                        account = UnionAddress(
                            BlockchainDto.IMMUTABLEX.group(), "0x6d13857ca83de08456b2b40aaf09a28e0aab056e"
                        ),
                        value = 1
                    )
                ),
            )
        )
    }

    @Test
    fun `should return ownership by item`(): Unit = runBlocking {
        val ownership = ImmutablexOwnershipService(
            mockk {
                coEvery {
                    getById(ImmutablexData.itemId_1)
                } returns ImmutablexData.item_1
            },

            mockk {
                coEvery {
                    getItemCreator(any())
                } returns "0x6d13857ca83de08456b2b40aaf09a28e0aab056e"

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
                            UnionAddress(BlockchainGroupDto.ETHEREUM, "0x6d13857ca83de08456b2b40aaf09a28e0aab056e")
                        ),
                        CollectionIdDto(BlockchainDto.IMMUTABLEX, ImmutablexData.contract_1),
                        BigInteger.ONE,
                        ImmutablexData.item_1.createdAt!!,
                        lazyValue = BigInteger.ZERO,
                        lastUpdatedAt = ImmutablexData.item_1.updatedAt!!,
                        creators = listOf(
                            CreatorDto(
                                account = UnionAddress(
                                    BlockchainDto.IMMUTABLEX.group(), "0x6d13857ca83de08456b2b40aaf09a28e0aab056e"
                                ),
                                value = 1
                            )
                        ),
                    )
                )
            )
        )
    }

}
