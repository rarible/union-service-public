package com.rarible.protocol.union.integration.ethereum.converter

import com.rarible.protocol.dto.Erc20DecimalBalanceDto
import com.rarible.protocol.dto.EthBalanceDto
import com.rarible.protocol.union.core.converter.UnionAddressConverter
import com.rarible.protocol.union.core.model.UnionBalance
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CurrencyIdDto

object EthBalanceConverter {

    const val NATIVE_CURRENCY_CONTRACT = "0x0000000000000000000000000000000000000000"

    fun convert(source: EthBalanceDto, blockchain: BlockchainDto): UnionBalance {
        return UnionBalance(
            currencyId = CurrencyIdDto(blockchain, NATIVE_CURRENCY_CONTRACT, null),
            owner = UnionAddressConverter.convert(blockchain, source.owner.prefixed()),
            balance = source.balance,
            decimal = source.decimalBalance
        )
    }

    fun convert(source: Erc20DecimalBalanceDto, blockchain: BlockchainDto): UnionBalance {
        return UnionBalance(
            currencyId = CurrencyIdDto(blockchain, source.contract.prefixed(), null),
            owner = UnionAddressConverter.convert(blockchain, source.owner.prefixed()),
            balance = source.balance,
            decimal = source.decimalBalance
        )
    }
}
