package com.rarible.protocol.union.core.converter.ethereum

import io.daonomic.rpc.domain.Bytes
import org.springframework.core.convert.converter.Converter

object EthBytesConverter : Converter<Bytes, String> {
    override fun convert(source: Bytes): String {
        return source.prefixed()
    }
}
