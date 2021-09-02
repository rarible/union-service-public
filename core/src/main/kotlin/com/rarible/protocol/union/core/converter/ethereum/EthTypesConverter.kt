package com.rarible.protocol.union.core.converter.ethereum

import io.daonomic.rpc.domain.Binary
import io.daonomic.rpc.domain.Word
import scalether.domain.Address

object EthTypesConverter {

    fun convert(address: Address) = address.prefixed()
    fun convert(word: Word) = word.prefixed()
    fun convert(binary: Binary) = binary.prefixed()

}