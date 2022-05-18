package com.rarible.protocol.union.integration.immutablex.converter

import com.rarible.protocol.union.dto.ActivityDto
import com.rarible.protocol.union.dto.ActivityIdDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.BlockchainGroupDto
import com.rarible.protocol.union.dto.BurnActivityDto
import com.rarible.protocol.union.dto.ContractAddress
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.L2DepositActivityDto
import com.rarible.protocol.union.dto.L2WithdrawalActivityDto
import com.rarible.protocol.union.dto.MintActivityDto
import com.rarible.protocol.union.dto.OrderActivitySourceDto
import com.rarible.protocol.union.dto.OrderMatchSellDto
import com.rarible.protocol.union.dto.TransferActivityDto
import com.rarible.protocol.union.dto.UnionAddress
import com.rarible.protocol.union.dto.group
import com.rarible.protocol.union.integration.immutablex.dto.ImmutablexDeposit
import com.rarible.protocol.union.integration.immutablex.dto.ImmutablexEvent
import com.rarible.protocol.union.integration.immutablex.dto.ImmutablexMint
import com.rarible.protocol.union.integration.immutablex.dto.ImmutablexTrade
import com.rarible.protocol.union.integration.immutablex.dto.ImmutablexTransfer
import com.rarible.protocol.union.integration.immutablex.dto.ImmutablexWithdrawal
import com.rarible.protocol.union.integration.immutablex.service.ImmutablexOrderService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import scalether.domain.Address

class ImmutablexEventConverter(
    private val orderService: ImmutablexOrderService
) {

    suspend fun convert(event: ImmutablexEvent, blockchain: BlockchainDto = BlockchainDto.IMMUTABLEX): ActivityDto {
        val id = ActivityIdDto(blockchain, "${event.transactionId}.${event.timestamp.toEpochMilli()}")
        return when(event) {
            is ImmutablexMint -> MintActivityDto(
                id = id,
                date = event.timestamp,
                owner = UnionAddress(blockchain.group(), event.user),
                contract = ContractAddress(blockchain, event.token.data.tokenAddress.orEmpty()),
                tokenId = event.token.data.tokenId(),
                value = event.token.data.quantity,
                transactionHash = "${event.transactionId}",
            )

            is ImmutablexTransfer -> {
                val from = UnionAddress(blockchain.group(), event.user)
                val to = UnionAddress(blockchain.group(), event.receiver)
                val contract = ContractAddress(blockchain, event.token.data.tokenAddress)
                if (to.value == Address.ZERO().toString()) {
                    BurnActivityDto(
                        id = id,
                        date = event.timestamp,
                        owner = from,
                        contract = contract,
                        tokenId = event.token.data.tokenId(),
                        value = event.token.data.quantity,
                        transactionHash = "${event.transactionId}"
                    )
                } else {
                    TransferActivityDto(
                        id = id,
                        date = event.timestamp,
                        from = from,
                        owner = to,
                        contract = contract,
                        tokenId = event.token.data.tokenId(),
                        value = event.token.data.quantity,
                        transactionHash = "${event.transactionId}"
                    )
                }
            }
            is ImmutablexDeposit -> L2DepositActivityDto(
                id = id, date = event.timestamp,
                user = UnionAddress(blockchain.group(), event.user),
                status = event.status, itemId = ItemIdDto(blockchain, event.token.data.tokenAddress, event.token.data.tokenId()), value = event.token.data.quantity
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
                id = id,
                date = event.timestamp,
                user = UnionAddress(BlockchainGroupDto.IMMUTABLEX, event.sender),
                status = event.status,
                itemId = ItemIdDto(blockchain, event.token.data.tokenAddress, event.token.data.tokenId()),
                value = event.token.data.quantity
            )
        }

    }
}
