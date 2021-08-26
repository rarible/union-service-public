package com.rarible.protocol.union.core.converter.ethereum

import com.rarible.protocol.dto.PartDto
import com.rarible.protocol.union.dto.EthRoyaltyDto
import org.springframework.core.convert.converter.Converter

object EthRoyaltyDtoConverter : Converter<PartDto, EthRoyaltyDto> {
    override fun convert(source: PartDto): EthRoyaltyDto {
        return EthRoyaltyDto(
            account = EthAddressConverter.convert(source.account),
            value = source.value.toBigInteger()
        )
    }
}
