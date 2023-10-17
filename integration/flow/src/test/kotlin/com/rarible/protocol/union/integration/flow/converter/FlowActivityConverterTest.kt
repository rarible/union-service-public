package com.rarible.protocol.union.integration.flow.converter

import com.rarible.protocol.dto.FlowOrderActivityMatchSideDto
import com.rarible.protocol.union.core.converter.UnionAddressConverter
import com.rarible.protocol.union.core.model.UnionBurnActivity
import com.rarible.protocol.union.core.model.UnionFlowAssetTypeFt
import com.rarible.protocol.union.core.model.UnionFlowAssetTypeNft
import com.rarible.protocol.union.core.model.UnionMintActivity
import com.rarible.protocol.union.core.model.UnionOrderActivityMatchSideDto
import com.rarible.protocol.union.core.model.UnionOrderCancelListActivity
import com.rarible.protocol.union.core.model.UnionOrderListActivity
import com.rarible.protocol.union.core.model.UnionOrderMatchActivity
import com.rarible.protocol.union.core.model.UnionOrderMatchSell
import com.rarible.protocol.union.core.model.UnionOrderMatchSwap
import com.rarible.protocol.union.core.model.UnionTransferActivity
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.OrderActivitySourceDto
import com.rarible.protocol.union.integration.flow.data.randomFlowBurnDto
import com.rarible.protocol.union.integration.flow.data.randomFlowCancelListActivityDto
import com.rarible.protocol.union.integration.flow.data.randomFlowFungibleAsset
import com.rarible.protocol.union.integration.flow.data.randomFlowMintDto
import com.rarible.protocol.union.integration.flow.data.randomFlowNftAsset
import com.rarible.protocol.union.integration.flow.data.randomFlowNftOrderActivityListDto
import com.rarible.protocol.union.integration.flow.data.randomFlowNftOrderActivitySell
import com.rarible.protocol.union.integration.flow.data.randomFlowOrderActivityMatchSideDto
import com.rarible.protocol.union.integration.flow.data.randomFlowTransferDto
import com.rarible.protocol.union.test.mock.CurrencyMock
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class FlowActivityConverterTest {

    val converter = FlowActivityConverter(CurrencyMock.currencyServiceMock)

    @Test
    fun `flow order activity match - swap`() = runBlocking<Unit> {
        val dto = randomFlowNftOrderActivitySell().copy(
            left = randomFlowOrderActivityMatchSideDto(randomFlowFungibleAsset()),
            right = randomFlowOrderActivityMatchSideDto(randomFlowFungibleAsset())
        )
        val converted = converter.convert(dto) as UnionOrderMatchActivity

        assertThat(converted.id.value).isEqualTo(dto.id)
        assertThat(converted.date).isEqualTo(dto.date)

        assertThat(converted).isInstanceOf(UnionOrderMatchSwap::class.java)
        converted as UnionOrderMatchSwap

        assertMatchSide(converted.left, dto.left)
        assertMatchSide(converted.right, dto.right)

        assertThat(converted.source).isEqualTo(OrderActivitySourceDto.RARIBLE)
        assertThat(converted.transactionHash).isEqualTo(dto.transactionHash)
    }

    @Test
    fun `flow order activity match - nft to payment`() = runBlocking<Unit> {
        val dto = randomFlowNftOrderActivitySell()
        val converted = converter.convert(dto) as UnionOrderMatchActivity

        assertThat(converted).isInstanceOf(UnionOrderMatchSell::class.java)
        converted as UnionOrderMatchSell

        assertThat(converted.id.value).isEqualTo(dto.id)
        assertThat(converted.date).isEqualTo(dto.date)

        assertThat(converted.type).isEqualTo(UnionOrderMatchSell.Type.SELL)
        assertThat(converted.nft).isEqualTo(FlowConverter.convert(dto.left.asset, BlockchainDto.FLOW))
        assertThat(converted.payment).isEqualTo(FlowConverter.convert(dto.right.asset, BlockchainDto.FLOW))
        assertThat(converted.seller).isEqualTo(
            UnionAddressConverter.convert(BlockchainDto.FLOW, dto.left.maker)
        )
        assertThat(converted.buyer).isEqualTo(
            UnionAddressConverter.convert(BlockchainDto.FLOW, dto.right.maker)
        )
        assertThat(converted.price).isEqualTo(dto.price)
        // Converted 1 to 1 to USD
        assertThat(converted.priceUsd).isEqualTo(dto.price)
        assertThat(converted.amountUsd).isEqualTo(dto.price.multiply(dto.left.asset.value))

        // TODO FLOW replace when Flow Implement it
        assertThat(converted.sellerOrderHash).isNull()
        assertThat(converted.buyerOrderHash).isNull()

        assertThat(converted.transactionHash).isEqualTo(dto.transactionHash)
        // TODO UNION remove in 1.19
        assertThat(converted.blockchainInfo!!.transactionHash).isEqualTo(dto.transactionHash)
        assertThat(converted.blockchainInfo!!.blockHash).isEqualTo(dto.blockHash)
        assertThat(converted.blockchainInfo!!.blockNumber).isEqualTo(dto.blockNumber)
        assertThat(converted.blockchainInfo!!.logIndex).isEqualTo(dto.logIndex)
    }

    @Test
    fun `flow order activity match - payment to nft`() = runBlocking<Unit> {
        val dto = randomFlowNftOrderActivitySell().copy(
            left = randomFlowOrderActivityMatchSideDto(randomFlowFungibleAsset()),
            right = randomFlowOrderActivityMatchSideDto(randomFlowNftAsset())
        )
        val converted = converter.convert(dto) as UnionOrderMatchActivity

        assertThat(converted).isInstanceOf(UnionOrderMatchSell::class.java)
        converted as UnionOrderMatchSell

        assertThat(converted.id.value).isEqualTo(dto.id)
        assertThat(converted.date).isEqualTo(dto.date)

        assertThat(converted.type).isEqualTo(UnionOrderMatchSell.Type.ACCEPT_BID)
        assertThat(converted.nft).isEqualTo(FlowConverter.convert(dto.right.asset, BlockchainDto.FLOW))
        assertThat(converted.payment).isEqualTo(FlowConverter.convert(dto.left.asset, BlockchainDto.FLOW))
        assertThat(converted.seller).isEqualTo(
            UnionAddressConverter.convert(BlockchainDto.FLOW, dto.right.maker)
        )
        assertThat(converted.buyer).isEqualTo(
            UnionAddressConverter.convert(BlockchainDto.FLOW, dto.left.maker)
        )
        assertThat(converted.price).isEqualTo(dto.price)
        // Converted 1 to 1 to USD
        assertThat(converted.priceUsd).isEqualTo(dto.price)
        assertThat(converted.amountUsd).isEqualTo(dto.price.multiply(dto.right.asset.value))

        // TODO FLOW replace when Flow Implement it
        assertThat(converted.sellerOrderHash).isNull()
        assertThat(converted.buyerOrderHash).isNull()

        assertThat(converted.transactionHash).isEqualTo(dto.transactionHash)
        // TODO UNION remove in 1.19
        assertThat(converted.blockchainInfo!!.transactionHash).isEqualTo(dto.transactionHash)
        assertThat(converted.blockchainInfo!!.blockHash).isEqualTo(dto.blockHash)
        assertThat(converted.blockchainInfo!!.blockNumber).isEqualTo(dto.blockNumber)
        assertThat(converted.blockchainInfo!!.logIndex).isEqualTo(dto.logIndex)
    }

    @Test
    fun `flow order activity list`() = runBlocking<Unit> {
        val dto = randomFlowNftOrderActivityListDto()
        val converted = converter.convert(dto) as UnionOrderListActivity

        assertThat(converted.id.value).isEqualTo(dto.id)
        assertThat(converted.date).isEqualTo(dto.date)
        assertThat(converted.price).isEqualTo(dto.price)
        // Converted 1 to 1 to USD
        assertThat(converted.priceUsd).isEqualTo(dto.price)
        assertThat(converted.hash).isEqualTo(dto.hash)
        assertThat(converted.maker.value).isEqualTo(dto.maker)
        assertThat(converted.make.value).isEqualTo(dto.make.value)
        val makeType = converted.make.type as UnionFlowAssetTypeFt
        assertThat(makeType.contract.value).isEqualTo(dto.make.contract)
        assertThat(converted.take.value).isEqualTo(dto.take.value)
        val takeType = converted.take.type as UnionFlowAssetTypeFt
        assertThat(takeType.contract.value).isEqualTo(dto.take.contract)
    }

    @Test
    fun `flow order activity cancel list`() = runBlocking<Unit> {
        val dto = randomFlowCancelListActivityDto()
        val converted =
            converter.convert(dto) as UnionOrderCancelListActivity

        assertThat(converted.id.value).isEqualTo(dto.id)
        assertThat(converted.date).isEqualTo(dto.date)
        assertThat(converted.hash).isEqualTo(dto.hash)
        assertThat(converted.maker.value).isEqualTo(dto.maker)
        val makeType = converted.make as UnionFlowAssetTypeNft
        assertThat(makeType.contract.value).isEqualTo(dto.make.contract)
        val takeType = converted.take as UnionFlowAssetTypeFt
        assertThat(takeType.contract.value).isEqualTo(dto.take.contract)
        assertThat(converted.transactionHash).isEqualTo(dto.transactionHash)
        assertThat(converted.blockchainInfo!!.transactionHash).isEqualTo(dto.transactionHash)
    }

    @Test
    fun `flow item activity mint`() = runBlocking<Unit> {
        val dto = randomFlowMintDto()
        val converted = converter.convert(dto) as UnionMintActivity

        assertThat(converted.id.value).isEqualTo(dto.id)
        assertThat(converted.date).isEqualTo(dto.date)
        assertThat(converted.owner.value).isEqualTo(dto.owner)
        assertThat(converted.contract!!.value).isEqualTo(dto.contract)
        assertThat(converted.collection!!.value).isEqualTo(dto.contract)
        assertThat(converted.tokenId).isEqualTo(dto.tokenId)
        assertThat(converted.itemId).isEqualTo(ItemIdDto(BlockchainDto.FLOW, dto.contract, dto.tokenId))
        assertThat(converted.value).isEqualTo(dto.value)
        assertThat(converted.transactionHash).isEqualTo(dto.transactionHash)
        // TODO UNION remove in 1.19
        assertThat(converted.blockchainInfo!!.transactionHash).isEqualTo(dto.transactionHash)
        assertThat(converted.blockchainInfo!!.blockHash).isEqualTo(dto.blockHash)
        assertThat(converted.blockchainInfo!!.blockNumber).isEqualTo(dto.blockNumber)
        assertThat(converted.blockchainInfo!!.logIndex).isEqualTo(dto.logIndex)
    }

    @Test
    fun `flow item activity transfer`() = runBlocking<Unit> {
        val dto = randomFlowTransferDto()
        val converted = converter.convert(dto) as UnionTransferActivity

        assertThat(converted.id.value).isEqualTo(dto.id)
        assertThat(converted.date).isEqualTo(dto.date)
        assertThat(converted.owner.value).isEqualTo(dto.owner)
        assertThat(converted.contract!!.value).isEqualTo(dto.contract)
        assertThat(converted.collection!!.value).isEqualTo(dto.contract)
        assertThat(converted.tokenId).isEqualTo(dto.tokenId)
        assertThat(converted.itemId).isEqualTo(ItemIdDto(BlockchainDto.FLOW, dto.contract, dto.tokenId))
        assertThat(converted.value).isEqualTo(dto.value)
        assertThat(converted.transactionHash).isEqualTo(dto.transactionHash)
        // TODO UNION remove in 1.19
        assertThat(converted.blockchainInfo!!.transactionHash).isEqualTo(dto.transactionHash)
        assertThat(converted.blockchainInfo!!.blockHash).isEqualTo(dto.blockHash)
        assertThat(converted.blockchainInfo!!.blockNumber).isEqualTo(dto.blockNumber)
        assertThat(converted.blockchainInfo!!.logIndex).isEqualTo(dto.logIndex)
    }

    @Test
    fun `flow item activity burn`() = runBlocking<Unit> {
        val dto = randomFlowBurnDto()
        val converted = converter.convert(dto) as UnionBurnActivity

        assertThat(converted.id.value).isEqualTo(dto.id)
        assertThat(converted.date).isEqualTo(dto.date)
        assertThat(converted.owner.value).isEqualTo(dto.owner)
        assertThat(converted.contract!!.value).isEqualTo(dto.contract)
        assertThat(converted.collection!!.value).isEqualTo(dto.contract)
        assertThat(converted.tokenId).isEqualTo(dto.tokenId)
        assertThat(converted.itemId).isEqualTo(ItemIdDto(BlockchainDto.FLOW, dto.contract, dto.tokenId))
        assertThat(converted.value).isEqualTo(dto.value)
        assertThat(converted.transactionHash).isEqualTo(dto.transactionHash)
        // TODO UNION remove in 1.19
        assertThat(converted.blockchainInfo!!.transactionHash).isEqualTo(dto.transactionHash)
        assertThat(converted.blockchainInfo!!.blockHash).isEqualTo(dto.blockHash)
        assertThat(converted.blockchainInfo!!.blockNumber).isEqualTo(dto.blockNumber)
        assertThat(converted.blockchainInfo!!.logIndex).isEqualTo(dto.logIndex)
    }

    @Test
    fun `flow order activity bid`() = runBlocking<Unit> {
        val dto = randomFlowNftOrderActivityListDto()
        val converted = converter.convert(dto) as UnionOrderListActivity

        assertThat(converted.id.value).isEqualTo(dto.id)
        assertThat(converted.date).isEqualTo(dto.date)
        assertThat(converted.price).isEqualTo(dto.price)
        // Converted 1 to 1 to USD
        assertThat(converted.priceUsd).isEqualTo(dto.price)
        assertThat(converted.hash).isEqualTo(dto.hash)
        assertThat(converted.maker.value).isEqualTo(dto.maker)
        assertThat(converted.make.value).isEqualTo(dto.make.value)
        val makeType = converted.make.type as UnionFlowAssetTypeFt
        assertThat(makeType.contract.value).isEqualTo(dto.make.contract)
        assertThat(converted.take.value).isEqualTo(dto.take.value)
        val takeType = converted.take.type as UnionFlowAssetTypeFt
        assertThat(takeType.contract.value).isEqualTo(dto.take.contract)
    }

    @Test
    fun `flow order activity cancel bid`() = runBlocking<Unit> {
        val dto = randomFlowCancelListActivityDto()
        val converted =
            converter.convert(dto) as UnionOrderCancelListActivity

        assertThat(converted.id.value).isEqualTo(dto.id)
        assertThat(converted.date).isEqualTo(dto.date)
        assertThat(converted.hash).isEqualTo(dto.hash)
        assertThat(converted.maker.value).isEqualTo(dto.maker)
        val makeType = converted.make as UnionFlowAssetTypeNft
        assertThat(makeType.contract.value).isEqualTo(dto.make.contract)
        val takeType = converted.take as UnionFlowAssetTypeFt
        assertThat(takeType.contract.value).isEqualTo(dto.take.contract)
        assertThat(converted.transactionHash).isEqualTo(dto.transactionHash)
        assertThat(converted.blockchainInfo!!.transactionHash).isEqualTo(dto.transactionHash)
    }

    private fun assertMatchSide(
        dest: UnionOrderActivityMatchSideDto,
        expected: FlowOrderActivityMatchSideDto
    ) {
        // TODO FLOW update when flow start to send it
        assertThat(dest.hash).isNull()
        assertThat(dest.maker.value).isEqualTo(expected.maker)
    }
}
