package com.rarible.protocol.union.integration.immutablex.converter

import com.rarible.protocol.union.dto.*
import com.rarible.protocol.union.integration.immutablex.dto.*
import com.rarible.protocol.union.integration.immutablex.service.ImmutablexOrderService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import scalether.domain.Address
import java.math.BigInteger

class ImmutablexEventConverter(
    private val orderService: ImmutablexOrderService
) {

    suspend fun convert(event: ImmutablexEvent): ActivityDto {
        val id = ActivityIdDto(BlockchainDto.IMMUTABLEX, "${event.transactionId}.${event.timestamp.toEpochMilli()}")
        return when(event) {
            is ImmutablexMint -> MintActivityDto(
                id = id,
                date = event.timestamp,
                owner = UnionAddress(BlockchainGroupDto.IMMUTABLEX, event.user),
                contract = ContractAddress(BlockchainDto.IMMUTABLEX, event.token.data.tokenAddress.orEmpty()),
                tokenId = event.token.data.tokenId?.toBigInteger() ?: BigInteger.ZERO,
                value = event.token.data.quantity?.toBigInteger() ?: BigInteger.ONE,
                transactionHash = "${event.transactionId}",
            )

            is ImmutablexTransfer -> {
                val from = UnionAddress(BlockchainGroupDto.IMMUTABLEX, event.user)
                val to = UnionAddress(BlockchainGroupDto.IMMUTABLEX, event.receiver)
                val contract = ContractAddress(BlockchainDto.IMMUTABLEX, event.token.data.tokenAddress!!)
                if (to.value == "${Address.ZERO()}") {
                    BurnActivityDto(
                        id = id,
                        date = event.timestamp,
                        owner = from,
                        contract = contract,
                        tokenId = event.token.data.tokenId!!.toBigInteger(),
                        value = event.token.data.quantity!!.toBigInteger(),
                        transactionHash = "${event.transactionId}"
                    )
                } else {
                    TransferActivityDto(
                        id = id,
                        date = event.timestamp,
                        from = from,
                        owner = to,
                        contract = contract,
                        tokenId = event.token.data.tokenId!!.toBigInteger(),
                        value = event.token.data.quantity!!.toBigInteger(),
                        transactionHash = "${event.transactionId}"
                    )
                }
            }
            is ImmutablexDeposit -> L2DepositActivityDto(
                id = id, date = event.timestamp,
                user = UnionAddress(BlockchainGroupDto.IMMUTABLEX, event.user),
                status = event.status, contractAddres = ContractAddress(BlockchainDto.IMMUTABLEX, event.token.data.tokenAddress!!),
                tokenId = event.token.data.tokenId!!.toBigInteger(), value = event.token.data.quantity?.toBigInteger()
            )
            is ImmutablexTrade -> {
                val (makeOrder, takeOrder) = runBlocking(Dispatchers.IO) {
                    orderService.getOrderById("${event.make.orderId}") to orderService.getOrderById("${event.take.orderId}")
                }

                OrderMatchSellDto(
                    id = id, source = OrderActivitySourceDto.IMMUTABLEX,
                    transactionHash = "${event.transactionId}",
                    date = event.timestamp,
                    nft = makeOrder.make,
                    payment = takeOrder.make,
                    buyer = makeOrder.maker,
                    seller = takeOrder.maker,
                    price = makeOrder.makePrice!!,
                    type = OrderMatchSellDto.Type.SELL
                )
            }
            is ImmutablexWithdrawal -> L2WithdrawalActivityDto(
                id = id, date = event.timestamp,
                user = UnionAddress(BlockchainGroupDto.IMMUTABLEX, event.sender),
                status = event.status, contractAddres = ContractAddress(BlockchainDto.IMMUTABLEX, event.token.data.tokenAddress!!),
                tokenId = event.token.data.tokenId!!.toBigInteger(), value = event.token.data.quantity?.toBigInteger()
            )
        }

    }
}
