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
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import java.math.BigInteger

internal class ImxOwnershipServiceTest {

    @Test
    fun `should return ownership by item`(): Unit = runBlocking {
        val ownership = ImxOwnershipService(
            mockk {
                coEvery {
                    getById(ImxData.itemId_1)
                } returns ImxData.item_1
            },

            mockk {
                coEvery {
                    getItemCreator(any())
                } returns "0x6d13857ca83de08456b2b40aaf09a28e0aab056e"

            }).getOwnershipsByItem(
            ImxData.itemId_1, null, 100
        )

        Assertions.assertThat(ownership).isEqualTo(
            Page(
                0,
                null,
                listOf(
                    UnionOwnership(
                        OwnershipIdDto(
                            BlockchainDto.IMMUTABLEX,
                            ImxData.itemId_1,
                            UnionAddress(BlockchainGroupDto.ETHEREUM, "0x6d13857ca83de08456b2b40aaf09a28e0aab056e")
                        ),
                        CollectionIdDto(BlockchainDto.IMMUTABLEX, ImxData.contract_1),
                        BigInteger.ONE,
                        ImxData.item_1.createdAt!!,
                        lazyValue = BigInteger.ZERO,
                        lastUpdatedAt = ImxData.item_1.updatedAt!!,
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
