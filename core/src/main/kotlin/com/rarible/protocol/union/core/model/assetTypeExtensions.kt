package com.rarible.protocol.union.core.model

import com.rarible.protocol.union.core.ethereum.converter.EthConverter
import com.rarible.protocol.union.dto.AssetTypeDto
import com.rarible.protocol.union.dto.EthCryptoPunksAssetTypeDto
import com.rarible.protocol.union.dto.EthErc1155AssetTypeDto
import com.rarible.protocol.union.dto.EthErc1155LazyAssetTypeDto
import com.rarible.protocol.union.dto.EthErc20AssetTypeDto
import com.rarible.protocol.union.dto.EthErc721AssetTypeDto
import com.rarible.protocol.union.dto.EthErc721LazyAssetTypeDto
import com.rarible.protocol.union.dto.EthEthereumAssetTypeDto
import com.rarible.protocol.union.dto.EthGenerativeArtAssetTypeDto
import com.rarible.protocol.union.dto.FlowAssetTypeFtDto
import com.rarible.protocol.union.dto.FlowAssetTypeNftDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.TezosFA12AssetTypeDto
import com.rarible.protocol.union.dto.TezosFA2AssetTypeDto
import com.rarible.protocol.union.dto.TezosXTZAssetTypeDto
import com.rarible.protocol.union.dto.UnionAddress
import scalether.domain.Address
import java.math.BigInteger

val AssetTypeDto.ext: AssetTypeExtension
    get() = when (this) {
        //---- ETHEREUM - currencies
        is EthEthereumAssetTypeDto -> AssetTypeExtension(
            isNft = false,
            isCurrency = true,
            contract = EthConverter.convert(Address.ZERO()),
            itemId = null
        )
        is EthErc20AssetTypeDto -> AssetTypeExtension(
            isNft = false,
            isCurrency = true,
            contract = this.contract.value,
            itemId = null
        )
        //---- ETHEREUM - NFTs
        is EthCryptoPunksAssetTypeDto -> AssetTypeExtension(
            isNft = true,
            isCurrency = false,
            contract = this.contract.value,
            itemId = toItemId(this.contract, this.punkId.toBigInteger())
        )
        is EthErc1155AssetTypeDto -> AssetTypeExtension(
            isNft = true,
            isCurrency = false,
            contract = this.contract.value,
            itemId = toItemId(this.contract, this.tokenId)
        )
        is EthErc1155LazyAssetTypeDto -> AssetTypeExtension(
            isNft = true,
            isCurrency = false,
            contract = this.contract.value,
            itemId = toItemId(this.contract, this.tokenId)
        )
        is EthErc721AssetTypeDto -> AssetTypeExtension(
            isNft = true,
            isCurrency = false,
            contract = this.contract.value,
            itemId = toItemId(this.contract, this.tokenId)
        )
        is EthErc721LazyAssetTypeDto -> AssetTypeExtension(
            isNft = true,
            isCurrency = false,
            contract = this.contract.value,
            itemId = toItemId(this.contract, this.tokenId)
        )
        //---- ETHEREUM - other
        is EthGenerativeArtAssetTypeDto -> AssetTypeExtension(
            isNft = false,
            isCurrency = false,
            contract = this.contract.value,
            itemId = null
        )

        //---- FLOW
        is FlowAssetTypeFtDto -> AssetTypeExtension(
            isNft = false,
            isCurrency = true,
            contract = this.contract.value,
            itemId = null
        )
        is FlowAssetTypeNftDto -> AssetTypeExtension(
            isNft = true,
            isCurrency = false,
            contract = this.contract.value,
            itemId = toItemId(this.contract, this.tokenId)
        )

        //---- TEZOS - currencies
        is TezosXTZAssetTypeDto -> AssetTypeExtension(
            isNft = false,
            isCurrency = true,
            contract = "", // TODO add fake contract
            itemId = null
        )
        is TezosFA12AssetTypeDto -> AssetTypeExtension(
            isNft = false,
            isCurrency = true,
            contract = this.contract.value,
            itemId = null
        )
        //---- TEZOS - NFTs
        is TezosFA2AssetTypeDto -> AssetTypeExtension(
            isNft = true,
            isCurrency = false,
            contract = this.contract.value,
            itemId = toItemId(this.contract, this.tokenId)
        )
    }

private fun toItemId(contract: UnionAddress, tokenId: BigInteger): ItemIdDto {
    return ItemIdDto(
        blockchain = contract.blockchain,
        token = contract,
        tokenId = tokenId
    )
}