package com.rarible.protocol.union.core.converter.flow

import com.rarible.protocol.union.dto.FlowAddress
import org.springframework.core.convert.converter.Converter

object FlowAddressConverter : Converter<String, FlowAddress> {

    override fun convert(source: String): FlowAddress {
        return FlowAddress(source)
    }

}

