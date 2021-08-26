package com.rarible.protocol.union.core.converter.ethereum

import com.rarible.protocol.dto.PartDto
import com.rarible.protocol.union.dto.EthOrderOriginFeeDto
import org.springframework.core.convert.converter.Converter

object EthOrderOriginFeeDtoConverter : Converter<PartDto, EthOrderOriginFeeDto> {
    override fun convert(source: PartDto): EthOrderOriginFeeDto {
        return EthOrderOriginFeeDto(
            account = EthAddressConverter.convert(source.account),
            value = source.value.toBigInteger()
        )
    }
}
