package com.rarible.protocol.union.core.converter.ethereum

import com.rarible.protocol.dto.PartDto
import com.rarible.protocol.union.dto.EthCreatorDto
import org.springframework.core.convert.converter.Converter

object EthCreatorDtoConverter : Converter<PartDto, EthCreatorDto> {
    override fun convert(source: PartDto): EthCreatorDto {
        return EthCreatorDto(
            account = EthAddressConverter.convert(source.account),
            value = source.value.toBigDecimal()
        )
    }
}
