package com.rarible.protocol.union.core.converter.ethereum

import com.rarible.protocol.union.dto.EthAddress
import org.springframework.core.convert.converter.Converter
import scalether.domain.Address

object EthAddressConverter : Converter<Address, EthAddress> {
    override fun convert(source: Address): EthAddress {
        return EthAddress(source.prefixed())
    }
}

