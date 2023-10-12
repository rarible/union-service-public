package com.rarible.protocol.union.core.model

import com.rarible.protocol.union.dto.CreatorDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.RoyaltyDto
import java.math.BigInteger

sealed class UnionLazyItem {
    abstract val id: ItemIdDto
    abstract val uri: String
    abstract val creators: List<CreatorDto>
    abstract val royalties: List<RoyaltyDto>
    abstract val signatures: List<String>
}

data class UnionEthLazyItemErc721(
    override val id: ItemIdDto,
    override val uri: String,
    override val creators: List<CreatorDto>,
    override val royalties: List<RoyaltyDto>,
    override val signatures: List<String>,
) : UnionLazyItem()

data class UnionEthLazyItemErc1155(
    override val id: ItemIdDto,
    override val uri: String,
    override val creators: List<CreatorDto>,
    override val royalties: List<RoyaltyDto>,
    override val signatures: List<String>,
    val supply: BigInteger,
) : UnionLazyItem()
