package com.rarible.protocol.union.integration.tezos.service

import com.rarible.dipdup.client.TokenClient
import com.rarible.dipdup.client.exception.DipDupNotFound
import com.rarible.protocol.union.core.exception.UnionNotFoundException
import com.rarible.protocol.union.integration.tezos.dipdup.DipDupIntegrationProperties
import com.rarible.protocol.union.integration.tezos.dipdup.service.DipDupItemService
import com.rarible.protocol.union.integration.tezos.dipdup.service.DipDupRoyaltyService
import com.rarible.protocol.union.integration.tezos.dipdup.service.TzktItemService
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class TezosItemTest {

    private val tzktItemService: TzktItemService = mockk()
    private val dipDupItemClient: TokenClient = mockk()
    private val dipDupItemService: DipDupItemService = DipDupItemService(dipDupItemClient)
    private val dipDupRoyaltyService: DipDupRoyaltyService = mockk()
    private val properties: DipDupIntegrationProperties = mockk()

    private val service = TezosItemService(tzktItemService, dipDupItemService, dipDupRoyaltyService, properties)

    @BeforeEach
    fun `set up`() {
        coEvery { properties.useDipDupTokens } returns true
    }

    @Test
    fun `should throw not found meta`() = runBlocking<Unit> {
        val itemId = "KT1RuoaCbnZpMgdRpSoLfJUzSkGz1ZSiaYwj:520"
        coEvery { dipDupItemClient.getTokenMetaById(itemId) } throws DipDupNotFound("")

        assertThrows<UnionNotFoundException> {
            service.getItemMetaById(itemId)
        }
    }

}
