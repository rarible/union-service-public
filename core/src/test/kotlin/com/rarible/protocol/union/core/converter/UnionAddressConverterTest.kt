package com.rarible.protocol.union.core.converter

import com.rarible.core.test.data.randomString
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.BlockchainGroupDto
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class UnionAddressConverterTest {

    @Test
    fun `to address`() {
        val address = randomString()
        val polygonAddress = UnionAddressConverter.convert(BlockchainDto.POLYGON, address)

        assertThat(polygonAddress.value).isEqualTo(address)
        assertThat(polygonAddress.blockchainGroup).isEqualTo(BlockchainGroupDto.ETHEREUM)

        val ethereumAddress = UnionAddressConverter.convert(BlockchainDto.ETHEREUM, address)

        assertThat(ethereumAddress.value).isEqualTo(address)
        assertThat(ethereumAddress.blockchainGroup).isEqualTo(BlockchainGroupDto.ETHEREUM)
    }

}
