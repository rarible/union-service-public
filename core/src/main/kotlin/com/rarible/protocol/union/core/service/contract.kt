package com.rarible.protocol.union.core.service

import com.rarible.protocol.union.dto.AssetTypeDto
import com.rarible.protocol.union.dto.BlockchainDto
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
import com.rarible.protocol.union.dto.TezosFA12AssetTypeDto
import com.rarible.protocol.union.dto.TezosFA2AssetTypeDto
import com.rarible.protocol.union.dto.TezosXTZAssetTypeDto
import com.rarible.protocol.union.dto.UnionAddress
import scalether.domain.Address

val AssetTypeDto.contract: UnionAddress
    get() = when(this) {
        is EthEthereumAssetTypeDto -> UnionAddress(BlockchainDto.ETHEREUM, Address.ZERO().toString())
        is EthCryptoPunksAssetTypeDto -> this.contract
        is EthErc1155AssetTypeDto -> this.contract
        is EthErc1155LazyAssetTypeDto -> this.contract
        is EthErc20AssetTypeDto -> this.contract
        is EthErc721AssetTypeDto -> this.contract
        is EthErc721LazyAssetTypeDto -> this.contract
        is EthGenerativeArtAssetTypeDto -> this.contract
        is FlowAssetTypeFtDto -> this.contract
        is FlowAssetTypeNftDto -> this.contract
        is TezosFA12AssetTypeDto -> this.contract
        is TezosFA2AssetTypeDto -> this.contract
        is TezosXTZAssetTypeDto -> TODO("add some fake address for TEZOS")
    }