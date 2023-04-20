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
    JsonSubTypes.Type(name = "FLOW_NFT", value = UnionFlowAssetTypeNft::class),
    JsonSubTypes.Type(name = "FLOW_FT", value = UnionFlowAssetTypeFt::class),
    JsonSubTypes.Type(name = "XTZ", value = UnionTezosXTZAssetType::class),
    JsonSubTypes.Type(name = "TEZOS_FT", value = UnionTezosFTAssetType::class),
    JsonSubTypes.Type(name = "TEZOS_NFT", value = UnionTezosNFTAssetType::class),
    JsonSubTypes.Type(name = "TEZOS_MT", value = UnionTezosMTAssetType::class),
    JsonSubTypes.Type(name = "ETH", value = UnionEthEthereumAssetType::class),
    JsonSubTypes.Type(name = "ERC20", value = UnionEthErc20AssetType::class),
    JsonSubTypes.Type(name = "ERC721", value = UnionEthErc721AssetType::class),
    JsonSubTypes.Type(name = "ERC721_Lazy", value = UnionEthErc721LazyAssetType::class),
    JsonSubTypes.Type(name = "ERC1155", value = UnionEthErc1155AssetType::class),
    JsonSubTypes.Type(name = "ERC1155_Lazy", value = UnionEthErc1155LazyAssetType::class),
    JsonSubTypes.Type(name = "CRYPTO_PUNKS", value = UnionEthCryptoPunksAssetType::class),
    JsonSubTypes.Type(name = "GEN_ART", value = UnionEthGenerativeArtAssetType::class),
    JsonSubTypes.Type(name = "COLLECTION", value = UnionEthCollectionAssetType::class),
    JsonSubTypes.Type(name = "AMM_NFT", value = UnionEthAmmNftAssetType::class),
    JsonSubTypes.Type(name = "SOLANA_NFT", value = UnionSolanaNftAssetType::class),
    JsonSubTypes.Type(name = "SOLANA_FT", value = UnionSolanaFtAssetType::class),
    JsonSubTypes.Type(name = "SOLANA_SOL", value = UnionSolanaSolAssetType::class)
)
sealed class UnionAssetType {

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

data class UnionFlowAssetTypeNft(
    val contract: ContractAddress,
    val tokenId: BigInteger
) : UnionAssetType() {

    override fun isNft() = true
    override fun itemId() = toItemId(this.contract, this.tokenId)
    override fun collectionId() = toCollectionId(this.contract)
}

data class UnionFlowAssetTypeFt(
    val contract: ContractAddress
) : UnionAssetType() {

    override fun isCurrency() = true
    override fun currencyId() = this.contract.value
}

//------------------ TEZOS ------------------//

class UnionTezosXTZAssetType : UnionAssetType() {

    override fun isCurrency() = true
    override fun currencyId() = "XTZ"

    override fun equals(other: Any?) = other?.javaClass == UnionTezosXTZAssetType::class.java
    override fun hashCode(): Int = 1
}

data class UnionTezosFTAssetType(
    val contract: ContractAddress,
    val tokenId: BigInteger? = null
) : UnionAssetType() {

    override fun isCurrency() = true
    override fun currencyId() = tokenId?.let { "${this.contract.value}:${it}" } ?: this.contract.value
}

data class UnionTezosNFTAssetType(
    val contract: ContractAddress,
    val tokenId: BigInteger
) : UnionAssetType() {

    override fun isNft() = true
    override fun itemId() = toItemId(this.contract, this.tokenId)
    override fun collectionId() = toCollectionId(this.contract)

}

data class UnionTezosMTAssetType(
    val contract: ContractAddress,
    val tokenId: BigInteger
) : UnionAssetType() {

    override fun isNft() = true
    override fun itemId() = toItemId(this.contract, this.tokenId)
    override fun collectionId() = toCollectionId(this.contract)
}

//------------------ ETH ------------------//

data class UnionEthEthereumAssetType(
    val blockchain: BlockchainDto? = null
) : UnionAssetType() {

    override fun isNft() = false
    override fun currencyId() = "0x0000000000000000000000000000000000000000"
}

data class UnionEthErc20AssetType(
    val contract: ContractAddress
) : UnionAssetType() {

    override fun isCurrency() = true
    override fun currencyId() = this.contract.value

}

data class UnionEthErc721AssetType(
    val contract: ContractAddress,
    val tokenId: BigInteger
) : UnionAssetType() {

    override fun isNft() = true
    override fun itemId() = toItemId(this.contract, this.tokenId)
    override fun collectionId() = toCollectionId(this.contract)
}

data class UnionEthErc721LazyAssetType(
    val contract: ContractAddress,
    val tokenId: BigInteger,
    val uri: String,
    val creators: List<CreatorDto>,
    val royalties: List<RoyaltyDto>,
    val signatures: List<String>
) : UnionAssetType() {

    override fun isNft() = true
    override fun itemId() = toItemId(this.contract, this.tokenId)
    override fun collectionId() = toCollectionId(this.contract)
}

data class UnionEthErc1155AssetType(
    val contract: ContractAddress,
    val tokenId: BigInteger
) : UnionAssetType() {

    override fun isNft() = true
    override fun itemId() = toItemId(this.contract, this.tokenId)
    override fun collectionId() = toCollectionId(this.contract)
}

data class UnionEthErc1155LazyAssetType(
    val contract: ContractAddress,
    val tokenId: BigInteger,
    val uri: String,
    val supply: BigInteger,
    val creators: List<CreatorDto>,
    val royalties: List<RoyaltyDto>,
    val signatures: List<String>
) : UnionAssetType() {

    override fun isNft() = true
    override fun itemId() = toItemId(this.contract, this.tokenId)
    override fun collectionId() = toCollectionId(this.contract)

}

data class UnionEthCryptoPunksAssetType(
    val contract: ContractAddress,
    val tokenId: Int
) : UnionAssetType() {

    override fun isNft() = true
    override fun itemId() = toItemId(this.contract, this.tokenId.toBigInteger())
    override fun collectionId() = toCollectionId(this.contract)
}

data class UnionEthGenerativeArtAssetType(
    val contract: ContractAddress
) : UnionAssetType() {

    override fun isCollectionAsset() = true
    override fun collectionId() = toCollectionId(this.contract)
}

data class UnionEthCollectionAssetType(
    val contract: ContractAddress
) : UnionAssetType() {

    override fun isNft() = true
    override fun isCollectionAsset() = true
    override fun collectionId() = toCollectionId(this.contract)

}

data class UnionEthAmmNftAssetType(
    val contract: ContractAddress
) : UnionAssetType() {

    override fun isNft() = true
    override fun isCollectionAsset() = true
    override fun collectionId() = toCollectionId(this.contract)
}

//------------------ SOLANA ------------------//

data class UnionSolanaNftAssetType(
    val contract: ContractAddress? = null,
    val itemId: ItemIdDto
) : UnionAssetType() {

    override fun isNft() = true
    override fun itemId() = itemId
    override fun collectionId() = contract?.let { toCollectionId(it) }
}

data class UnionSolanaFtAssetType(
    val address: ContractAddress
) : UnionAssetType() {

    override fun isCurrency() = true
    override fun currencyId() = address.value
}

class UnionSolanaSolAssetType : UnionAssetType() {

    override fun isCurrency() = true
    override fun currencyId() = "So11111111111111111111111111111111111111112"

    override fun equals(other: Any?) = other?.javaClass == UnionSolanaSolAssetType::class.java
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