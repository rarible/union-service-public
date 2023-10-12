package com.rarible.protocol.union.integration.ethereum.converter

import com.rarible.core.test.data.randomBigInt
import com.rarible.core.test.data.randomBinary
import com.rarible.core.test.data.randomInt
import com.rarible.core.test.data.randomString
import com.rarible.protocol.dto.LazyErc1155Dto
import com.rarible.protocol.dto.LazyErc721Dto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CreatorDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.LazyItemBurnFormDto
import com.rarible.protocol.union.dto.LazyItemMintFormDto
import com.rarible.protocol.union.dto.RoyaltyDto
import com.rarible.protocol.union.integration.ethereum.data.randomAddressString
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import randomEthLazyItemErc1155Dto
import randomEthLazyItemErc721Dto
import randomUnionAddress

class UnionItemConverterTest {

    @Test
    fun `convert - ok, erc721`() {
        val creator = CreatorDto(randomUnionAddress(), randomInt())
        val royalty = RoyaltyDto(randomUnionAddress(), randomInt())
        val contract = randomAddressString()
        val tokenId = randomBigInt()
        val dto = randomEthLazyItemErc721Dto(
            itemId = ItemIdDto(BlockchainDto.ETHEREUM, contract, tokenId),
            uri = randomString(),
            creators = listOf(creator),
            royalties = listOf(royalty),
            signatures = listOf(randomBinary().prefixed())
        )

        val result = UnionItemConverter.convert(LazyItemMintFormDto(dto)) as LazyErc721Dto

        assertThat(result.uri).isEqualTo(dto.uri)
        assertThat(result.contract.prefixed()).isEqualTo(contract)
        assertThat(result.tokenId).isEqualTo(tokenId)
        assertThat(result.creators).hasSize(1)
        assertThat(result.creators[0].value).isEqualTo(dto.creators[0].value)
        assertThat(result.creators[0].account.prefixed()).isEqualTo(dto.creators[0].account.value)
        assertThat(result.royalties).hasSize(1)
        assertThat(result.royalties[0].value).isEqualTo(dto.royalties[0].value)
        assertThat(result.royalties[0].account.prefixed()).isEqualTo(dto.royalties[0].account.value)
        assertThat(result.signatures.map { it.prefixed() }).isEqualTo(dto.signatures)
    }

    @Test
    fun `convert - ok, erc1155`() {
        val creator = CreatorDto(randomUnionAddress(), randomInt())
        val royalty = RoyaltyDto(randomUnionAddress(), randomInt())
        val contract = randomAddressString()
        val tokenId = randomBigInt()
        val dto = randomEthLazyItemErc1155Dto(
            itemId = ItemIdDto(BlockchainDto.ETHEREUM, contract, tokenId),
            creators = listOf(creator),
            royalties = listOf(royalty),
            signatures = listOf(randomBinary().prefixed()),
        )

        val result = UnionItemConverter.convert(LazyItemMintFormDto(dto)) as LazyErc1155Dto

        assertThat(result.uri).isEqualTo(dto.uri)
        assertThat(result.contract.prefixed()).isEqualTo(contract)
        assertThat(result.tokenId).isEqualTo(tokenId)
        assertThat(result.supply).isEqualTo(dto.supply)
        assertThat(result.creators).hasSize(1)
        assertThat(result.creators[0].value).isEqualTo(dto.creators[0].value)
        assertThat(result.creators[0].account.prefixed()).isEqualTo(dto.creators[0].account.value)
        assertThat(result.royalties).hasSize(1)
        assertThat(result.royalties[0].value).isEqualTo(dto.royalties[0].value)
        assertThat(result.royalties[0].account.prefixed()).isEqualTo(dto.royalties[0].account.value)
        assertThat(result.signatures.map { it.prefixed() }).isEqualTo(dto.signatures)
    }

    @Test
    fun `convert - ok, burn form`() {
        val creator = randomUnionAddress()
        val contract = randomAddressString()
        val tokenId = randomBigInt()
        val dto = LazyItemBurnFormDto(
            id = ItemIdDto(BlockchainDto.ETHEREUM, contract, tokenId),
            creators = listOf(creator),
            signatures = listOf(randomBinary().prefixed()),
        )

        val result = UnionItemConverter.convert(dto)

        assertThat(result.creators).hasSize(1)
        assertThat(result.creators[0].prefixed()).isEqualTo(dto.creators[0].value)
        assertThat(result.signatures.map { it.prefixed() }).isEqualTo(dto.signatures)
    }
}
