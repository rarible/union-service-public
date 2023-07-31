package com.rarible.protocol.union.core.converter

import com.rarible.protocol.union.core.exception.ContractFormatException
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.ContractAddress
import org.apache.commons.lang3.StringUtils

object ContractAddressConverter {

    private val chars = ": ".toCharArray()

    fun convert(blockchain: BlockchainDto, source: String): ContractAddress {
        validate(blockchain, source)
        return ContractAddress(blockchain, source)
    }

    fun validate(blockchain: BlockchainDto, source: String) {
        // TODO UNION maybe we need here smarter validation?
        if (StringUtils.containsAny(source, *chars)) {
            throw ContractFormatException(
                "ContractAddress value should not contains ':', but received: source=$source, group=${blockchain.name}"
            )
        }
    }
}
