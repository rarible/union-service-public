package com.rarible.protocol.union.core.flow.converter

import com.rarible.protocol.dto.FlowOrderDto
import com.rarible.protocol.dto.PayInfoDto
import com.rarible.protocol.union.core.converter.UnionAddressConverter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.FlowOrderDataV1Dto
import com.rarible.protocol.union.dto.FlowOrderPayoutDto
import com.rarible.protocol.union.dto.OrderDto
import com.rarible.protocol.union.dto.OrderIdDto
import com.rarible.protocol.union.dto.PlatformDto

object FlowOrderConverter {

    fun convert(order: FlowOrderDto, blockchain: BlockchainDto): OrderDto {
        return OrderDto(
            id = OrderIdDto(blockchain, order.id.toString()),
            platform = PlatformDto.RARIBLE,
            maker = UnionAddressConverter.convert(order.maker, blockchain),
            taker = order.taker?.let { UnionAddressConverter.convert(it, blockchain) },
            make = FlowConverter.convert(order.make, blockchain),
            take = FlowConverter.convert(order.take, blockchain),
            fill = order.fill,
            startedAt = order.start,
            endedAt = order.end,
            // TODO makeStock is needed to be BigDecimal on the flow client side
            makeStock = order.makeStock.toBigDecimal(),
            cancelled = order.cancelled,
            createdAt = order.createdAt,
            lastUpdatedAt = order.lastUpdateAt,
            makePriceUsd = order.priceUsd,
            takePriceUsd = order.priceUsd,
            priceHistory = emptyList(),
            data = convert(order.data, blockchain),
            salt = ""// TODO could be supported on Flow?
        )
    }

    private fun convert(
        source: com.rarible.protocol.dto.FlowOrderDataDto,
        blockchain: BlockchainDto
    ): FlowOrderDataV1Dto {
        return FlowOrderDataV1Dto(
            payouts = source.payouts.map { convert(it, blockchain) },
            originFees = source.originalFees.map { convert(it, blockchain) }
        )
    }

    private fun convert(source: PayInfoDto, blockchain: BlockchainDto): FlowOrderPayoutDto {
        return FlowOrderPayoutDto(
            account = UnionAddressConverter.convert(source.account, blockchain),
            value = source.value.toBigInteger()
        )
    }
}

