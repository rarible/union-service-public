package com.rarible.protocol.union.core.service

import com.rarible.core.test.data.randomBigInt
import com.rarible.core.test.data.randomString
import com.rarible.protocol.union.core.converter.UnionAddressConverter
import com.rarible.protocol.union.core.model.Restriction
import com.rarible.protocol.union.core.model.RestrictionApiRule
import com.rarible.protocol.union.core.model.RestrictionCheckResult
import com.rarible.protocol.union.core.model.RestrictionRule
import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.core.restriction.RestrictionApiRuleChecker
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.OwnershipRestrictionCheckFormDto
import com.rarible.protocol.union.dto.RestrictionTypeDto
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class RestrictionServiceTest {

    private val checker: RestrictionApiRuleChecker = mockk()
    private val itemService: ItemService = mockk()
    private lateinit var restrictionService: RestrictionService

    @BeforeEach
    fun beforeEach() {
        coEvery { itemService.blockchain } returns BlockchainDto.ETHEREUM
        val router = BlockchainRouter(listOf(itemService), listOf(BlockchainDto.ETHEREUM))
        restrictionService = RestrictionService(router, listOf(checker))
    }

    @Test
    fun `check ownership restriction`() = runBlocking<Unit> {
        val itemId = ItemIdDto(BlockchainDto.ETHEREUM, randomString(), randomBigInt())
        val user = randomString()
        val form = OwnershipRestrictionCheckFormDto(
            UnionAddressConverter.convert(BlockchainDto.ETHEREUM, user)
        )
        val rule = RestrictionApiRule(
            method = RestrictionApiRule.Method.GET,
            uriTemplate = randomString()
        )
        val meta = randomMeta(rule)

        val checkResult = RestrictionCheckResult(true)

        coEvery { itemService.getItemMetaById(itemId.value) } returns meta
        coEvery { checker.checkRule(itemId, rule, form) } returns RestrictionCheckResult(true)
        val result = restrictionService.checkRestriction(itemId, form)

        assertThat(result).isEqualTo(checkResult)
    }

    @Test
    fun `check item without restrictions`() = runBlocking<Unit> {
        val itemId = ItemIdDto(BlockchainDto.ETHEREUM, randomString(), randomBigInt())
        val user = randomString()
        val form = OwnershipRestrictionCheckFormDto(
            UnionAddressConverter.convert(BlockchainDto.ETHEREUM, user)
        )
        val meta = UnionMeta(
            name = randomString(),
            attributes = emptyList(),
            content = emptyList(),
            restrictions = listOf()
        )

        val expectedResult = RestrictionCheckResult(true)

        coEvery { itemService.getItemMetaById(itemId.value) } returns meta
        val result = restrictionService.checkRestriction(itemId, form)

        assertThat(result).isEqualTo(expectedResult)
    }

    private fun randomMeta(rule: RestrictionRule): UnionMeta {
        val restriction = Restriction(
            type = RestrictionTypeDto.OWNERSHIP,
            rule = rule
        )
        return UnionMeta(
            name = randomString(),
            attributes = emptyList(),
            content = emptyList(),
            restrictions = listOf(restriction)
        )
    }
}
