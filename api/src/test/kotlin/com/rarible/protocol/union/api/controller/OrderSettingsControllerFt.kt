package com.rarible.protocol.union.api.controller

import com.rarible.protocol.union.api.controller.test.AbstractIntegrationTest
import com.rarible.protocol.union.api.controller.test.IntegrationTest
import com.rarible.protocol.union.core.exception.UnionNotFoundException
import com.rarible.protocol.union.core.exception.UnionValidationException
import com.rarible.protocol.union.dto.BlockchainDto
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired

@FlowPreview
@IntegrationTest
class OrderSettingsControllerFt : AbstractIntegrationTest() {

    @Autowired
    lateinit var orderSettingsControllerClient: OrderSettingsControllerApi

    @Test
    fun `order fees - ethereum`() = runBlocking<Unit> {
        val response = orderSettingsControllerClient.getOrderFees(BlockchainDto.ETHEREUM).body

        assertThat(response.fees).hasSize(10)
        assertThat(response.fees["RARIBLE_V2"]).isEqualTo(10)
    }

    @Test
    fun `order fees - missed blockchain`() = runBlocking<Unit> {
        assertThrows<UnionValidationException> {
            orderSettingsControllerClient.getOrderFees(null)
        }
    }

    @Test
    fun `order fees - non-supported blockchain`() = runBlocking<Unit> {
        assertThrows<UnionNotFoundException> {
            orderSettingsControllerClient.getOrderFees(BlockchainDto.TEZOS)
        }
    }
}
