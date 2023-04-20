package com.rarible.protocol.union.core.model

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.ContractAddress
import com.rarible.protocol.union.dto.CreatorDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.RoyaltyDto
import java.math.BigInteger

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY)
@JsonSubTypes(
    JsonSubTypes.Type(name = "FLOW_NFT", value = UnionFlowAssetTypeNftDto::class),
    JsonSubTypes.Type(name = "FLOW_FT", value = UnionFlowAssetTypeFtDto::class),
    JsonSubTypes.Type(name = "XTZ", value = UnionTezosXTZAssetTypeDto::class),
    JsonSubTypes.Type(name = "TEZOS_FT", value = UnionTezosFTAssetTypeDto::class),
    JsonSubTypes.Type(name = "TEZOS_NFT", value = UnionTezosNFTAssetTypeDto::class),
    JsonSubTypes.Type(name = "TEZOS_MT", value = UnionTezosMTAssetTypeDto::class),
    JsonSubTypes.Type(name = "ETH", value = UnionEthEthereumAssetTypeDto::class),
    JsonSubTypes.Type(name = "ERC20", value = UnionEthErc20AssetTypeDto::class),
    JsonSubTypes.Type(name = "ERC721", value = UnionEthErc721AssetTypeDto::class),
    JsonSubTypes.Type(name = "ERC721_Lazy", value = UnionEthErc721LazyAssetTypeDto::class),
    JsonSubTypes.Type(name = "ERC1155", value = UnionEthErc1155AssetTypeDto::class),
    JsonSubTypes.Type(name = "ERC1155_Lazy", value = UnionEthErc1155LazyAssetTypeDto::class),
    JsonSubTypes.Type(name = "CRYPTO_PUNKS", value = UnionEthCryptoPunksAssetTypeDto::class),
    JsonSubTypes.Type(name = "GEN_ART", value = UnionEthGenerativeArtAssetTypeDto::class),
    JsonSubTypes.Type(name = "COLLECTION", value = UnionEthCollectionAssetTypeDto::class),
    JsonSubTypes.Type(name = "AMM_NFT", value = UnionEthAmmNftAssetTypeDto::class),
    JsonSubTypes.Type(name = "SOLANA_NFT", value = UnionSolanaNftAssetTypeDto::class),
    JsonSubTypes.Type(name = "SOLANA_FT", value = UnionSolanaFtAssetTypeDto::class),
    JsonSubTypes.Type(name = "SOLANA_SOL", value = UnionSolanaSolAssetTypeDto::class)
)
sealed class UnionAssetTypeDto {

    open fun isCurrency(): Boolean = false
    open fun isNft(): Boolean = false
    open fun isCollectionAsset(): Boolean = false

    open fun itemId(): ItemIdDto? = null

    /**
     * Item's collection id OR asset collection id (like "floor bid)
     */
    open fun collectionId(): CollectionIdDto? = null
    open fun currencyId(): String? = null

}

//------------------ FLOW ------------------//

data class UnionFlowAssetTypeNftDto(
    val contract: ContractAddress,
    val tokenId: BigInteger
) : UnionAssetTypeDto() {

    override fun isNft() = true
    override fun itemId() = toItemId(this.contract, this.tokenId)
    override fun collectionId() = toCollectionId(this.contract)
}

data class UnionFlowAssetTypeFtDto(
    val contract: ContractAddress
) : UnionAssetTypeDto() {

    override fun isCurrency() = true
    override fun currencyId() = this.contract.value
}

//------------------ TEZOS ------------------//

class UnionTezosXTZAssetTypeDto : UnionAssetTypeDto() {

    override fun isCurrency() = true
    override fun currencyId() = "XTZ"

    override fun equals(other: Any?) = other?.javaClass == UnionTezosXTZAssetTypeDto::class.java
    override fun hashCode(): Int = 1
}

data class UnionTezosFTAssetTypeDto(
    val contract: ContractAddress,
    val tokenId: BigInteger? = null
) : UnionAssetTypeDto() {

    override fun isCurrency() = true
    override fun currencyId() = tokenId?.let { "${this.contract.value}:${it}" } ?: this.contract.value
}

data class UnionTezosNFTAssetTypeDto(
    val contract: ContractAddress,
    val tokenId: BigInteger
) : UnionAssetTypeDto() {

    override fun isNft() = true
    override fun itemId() = toItemId(this.contract, this.tokenId)
    override fun collectionId() = toCollectionId(this.contract)

}

data class UnionTezosMTAssetTypeDto(
    val contract: ContractAddress,
    val tokenId: BigInteger
) : UnionAssetTypeDto() {

    override fun isNft() = true
    override fun itemId() = toItemId(this.contract, this.tokenId)
    override fun collectionId() = toCollectionId(this.contract)
}

//------------------ ETH ------------------//

data class UnionEthEthereumAssetTypeDto(
    val blockchain: BlockchainDto? = null
) : UnionAssetTypeDto() {

    override fun isNft() = false
    override fun currencyId() = "0x0000000000000000000000000000000000000000"
}

data class UnionEthErc20AssetTypeDto(
    val contract: ContractAddress
) : UnionAssetTypeDto() {

    override fun isCurrency() = true
    override fun currencyId() = this.contract.value

}

data class UnionEthErc721AssetTypeDto(
    val contract: ContractAddress,
    val tokenId: BigInteger
) : UnionAssetTypeDto() {

    override fun isNft() = true
    override fun itemId() = toItemId(this.contract, this.tokenId)
    override fun collectionId() = toCollectionId(this.contract)
}

data class UnionEthErc721LazyAssetTypeDto(
    val contract: ContractAddress,
    val tokenId: BigInteger,
    val uri: String,
    val creators: List<CreatorDto>,
    val royalties: List<RoyaltyDto>,
    val signatures: List<String>
) : UnionAssetTypeDto() {

    override fun isNft() = true
    override fun itemId() = toItemId(this.contract, this.tokenId)
    override fun collectionId() = toCollectionId(this.contract)
}

data class UnionEthErc1155AssetTypeDto(
    val contract: ContractAddress,
    val tokenId: BigInteger
) : UnionAssetTypeDto() {

    override fun isNft() = true
    override fun itemId() = toItemId(this.contract, this.tokenId)
    override fun collectionId() = toCollectionId(this.contract)
}

data class UnionEthErc1155LazyAssetTypeDto(
    val contract: ContractAddress,
    val tokenId: BigInteger,
    val uri: String,
    val supply: BigInteger,
    val creators: List<CreatorDto>,
    val royalties: List<RoyaltyDto>,
    val signatures: List<String>
) : UnionAssetTypeDto() {

    override fun isNft() = true
    override fun itemId() = toItemId(this.contract, this.tokenId)
    override fun collectionId() = toCollectionId(this.contract)

}

data class UnionEthCryptoPunksAssetTypeDto(
    val contract: ContractAddress,
    val tokenId: Int
) : UnionAssetTypeDto() {

    override fun isNft() = true
    override fun itemId() = toItemId(this.contract, this.tokenId.toBigInteger())
    override fun collectionId() = toCollectionId(this.contract)
}

data class UnionEthGenerativeArtAssetTypeDto(
    val contract: ContractAddress
) : UnionAssetTypeDto() {

    override fun isCollectionAsset() = true
    override fun collectionId() = toCollectionId(this.contract)
}

data class UnionEthCollectionAssetTypeDto(
    val contract: ContractAddress
) : UnionAssetTypeDto() {

    override fun isNft() = true
    override fun isCollectionAsset() = true
    override fun collectionId() = toCollectionId(this.contract)

}

data class UnionEthAmmNftAssetTypeDto(
    val contract: ContractAddress
) : UnionAssetTypeDto() {

    override fun isNft() = true
    override fun isCollectionAsset() = true
    override fun collectionId() = toCollectionId(this.contract)
}

//------------------ SOLANA ------------------//

data class UnionSolanaNftAssetTypeDto(
    val contract: ContractAddress? = null,
    val itemId: ItemIdDto
) : UnionAssetTypeDto() {

    override fun isNft() = true
    override fun itemId() = itemId
    override fun collectionId() = contract?.let { toCollectionId(it) }
}

data class UnionSolanaFtAssetTypeDto(
    val address: ContractAddress
) : UnionAssetTypeDto() {

    override fun isCurrency() = true
    override fun currencyId() = address.value
}

class UnionSolanaSolAssetTypeDto : UnionAssetTypeDto() {

    override fun isCurrency() = true
    override fun currencyId() = "So11111111111111111111111111111111111111112"

    override fun equals(other: Any?) = other?.javaClass == UnionSolanaSolAssetTypeDto::class.java
    override fun hashCode(): Int = 1
}

private fun toItemId(contract: ContractAddress, tokenId: BigInteger): ItemIdDto {
    return ItemIdDto(
        blockchain = contract.blockchain,
        value = "${contract.value}:$tokenId"
    )
}

private fun toCollectionId(contract: ContractAddress): CollectionIdDto {
    return CollectionIdDto(
        blockchain = contract.blockchain,
        value = contract.value
    )
}