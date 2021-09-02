package com.rarible.protocol.union.core.converter.flow

import com.rarible.protocol.union.dto.FlowContract
import org.springframework.core.convert.converter.Converter

object FlowContractConverter : Converter<String, FlowContract> {

    override fun convert(source: String): FlowContract {
        return FlowContract(source)
    }

}

