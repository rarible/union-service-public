package com.rarible.protocol.union.core.converter.ethereum

import com.rarible.protocol.dto.PartDto
import com.rarible.protocol.union.dto.EthOrderPayoutDto
import org.springframework.core.convert.converter.Converter

object EthOrderPayoutDtoConverter : Converter<PartDto, EthOrderPayoutDto> {
    override fun convert(source: PartDto): EthOrderPayoutDto {
        return EthOrderPayoutDto(
            account = EthAddressConverter.convert(source.account),
            value = source.value.toBigInteger()
        )
    }
}
