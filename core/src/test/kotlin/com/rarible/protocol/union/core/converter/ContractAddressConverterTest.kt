package com.rarible.protocol.union.core.converter

import com.rarible.protocol.union.core.exception.ContractFormatException
import com.rarible.protocol.union.dto.BlockchainDto
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class ContractAddressConverterTest {

    private val blockchain = BlockchainDto.ETHEREUM

    @Test
    fun `validate contract`() {
        assertThrows<ContractFormatException> {
            ContractAddressConverter.validate(blockchain, "Contract with spaces")
        }
        assertThrows<ContractFormatException> {
            ContractAddressConverter.validate(blockchain, "http://link.instead.of.contract")
        }
        ContractAddressConverter.validate(blockchain, "abcdefghiajklmonpqrstuvwxyz1234567890!@#$%^&*()_+|\\/.,;")
    }
}
