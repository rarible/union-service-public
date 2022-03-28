package com.rarible.protocol.union.core.domain

import com.rarible.contracts.erc1155.sale.v1.CloseOrderEvent
import com.rarible.contracts.erc1155.v1.SecondarySaleFeesEvent
import com.rarible.contracts.erc721.sale.old.BuyEvent
import com.rarible.contracts.erc721.sale.old.CancelEvent
import com.rarible.domain.ContractType.SALE
import com.rarible.domain.ContractType.TOKEN
import com.rarible.erc1155.TransferSingleEvent
import com.rarible.erc721.TransferEvent
import com.rarible.ethereum.listener.log.domain.EventData
import com.rarible.marketplace.core.model.BlockchainAddress
import io.daonomic.rpc.domain.Word
import java.math.BigDecimal
import java.util.Date

enum class ContractType {
    TOKEN, SALE
}

enum class ItemType(
    val topic: Set<Word>,
    val contract: ContractType
) {
    TRANSFER(setOf(TransferEvent.id(), TransferSingleEvent.id()), TOKEN),
    CANCEL(setOf(CancelEvent.id(), CloseOrderEvent.id(), com.rarible.contracts.exchange.v1.CancelEvent.id()), SALE),
    BUY(
        setOf(
            BuyEvent.id(),
            com.rarible.contracts.erc1155.sale.v1.BuyEvent.id(),
            com.rarible.contracts.exchange.v1.BuyEvent.id()
        ), SALE
    ),
    ROYALTY(setOf(SecondarySaleFeesEvent.id()), TOKEN)
}

abstract class ItemHistory : EventData {
    abstract val owner: BlockchainAddress?
    abstract val token: BlockchainAddress
    abstract val tokenId: String
    abstract val value: BigDecimal?
    abstract val type: ItemType
    abstract val date: Date
}

data class ItemTransfer(
    override val owner: BlockchainAddress,
    override val token: BlockchainAddress,
    override val tokenId: String,
    override val date: Date,
    val from: BlockchainAddress,
    override val value: BigDecimal = BigDecimal.ONE
) : ItemHistory() {
    override val type: ItemType
        get() = ItemType.TRANSFER
}

data class ItemRoyalty(
    override val token: BlockchainAddress,
    override val tokenId: String,
    override val date: Date,
    val royalties: List<Royalty>
) : ItemHistory() {
    override val owner: BlockchainAddress?
        get() = null
    override val type: ItemType
        get() = ItemType.ROYALTY
    override val value: BigDecimal?
        get() = null
}
