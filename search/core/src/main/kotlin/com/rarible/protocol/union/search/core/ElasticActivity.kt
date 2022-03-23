package com.rarible.protocol.union.search.core

import com.rarible.protocol.union.dto.ActivityTypeDto
import com.rarible.protocol.union.dto.AssetDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.OrderActivitySourceDto
import scalether.domain.Address
import java.math.BigDecimal
import java.math.BigInteger
import java.time.Instant

sealed class ElasticActivity {
    abstract val id: String
    // Sort fields
    abstract val date: Instant
    abstract val blockNumber: Long?
    abstract val logIndex: Long?
    // Filter fields
    abstract val blockchain: BlockchainDto
    abstract val type: ActivityTypeDto
    abstract val maker: String
    abstract val taker: String?
    abstract val collection: List<String>
    abstract val itemId: List<String>
}

data class ElasticMintActivity(
    override val id: String,

    override val date: Instant,
    override val blockNumber: Long,
    override val logIndex: Long,

    override val blockchain: BlockchainDto,
    override val type: ActivityTypeDto = ActivityTypeDto.MINT,
    override val maker: String,
    override val taker: String? = null,
    override val collection: List<String>,
    override val itemId: List<String>,
) : ElasticActivity()

data class ElasticBurnActivity(
    override val id: String,

    override val date: Instant,
    override val blockNumber: Long,
    override val logIndex: Long,

    override val blockchain: BlockchainDto,
    override val type: ActivityTypeDto = ActivityTypeDto.MINT,
    override val maker: String,
    override val taker: String? = null,
    override val collection: List<String>,
    override val itemId: List<String>,
) : ElasticActivity()

data class ElasticTransferActivity(
    override val id: String,

    override val date: Instant,
    override val blockNumber: Long,
    override val logIndex: Long,

    override val blockchain: BlockchainDto,
    override val type: ActivityTypeDto = ActivityTypeDto.BURN,
    override val maker: String,
    override val taker: String,
    override val collection: List<String>,
    override val itemId: List<String>,
) : ElasticActivity()

data class ElasticOrderMatchActivity(
    override val id: String,

    override val date: Instant,
    override val blockNumber: Long,
    override val logIndex: Long,

    override val blockchain: BlockchainDto,
    override val type: ActivityTypeDto = ActivityTypeDto.SELL,
    override val maker: String,
    override val taker: String,
    override val collection: List<String>,
    override val itemId: List<String>,
) : ElasticActivity()

data class ElasticOrderBidActivity(
    override val id: String,

    override val date: Instant,
    override val blockNumber: Long?,
    override val logIndex: Long?,

    override val blockchain: BlockchainDto,
    override val type: ActivityTypeDto = ActivityTypeDto.BID,
    override val maker: String,
    override val taker: String? = null,
    override val collection: List<String>,
    override val itemId: List<String>,
) : ElasticActivity()

data class ElasticOrderListActivity(
    override val id: String,

    override val date: Instant,
    override val blockNumber: Long?,
    override val logIndex: Long?,

    override val blockchain: BlockchainDto,
    override val type: ActivityTypeDto = ActivityTypeDto.LIST,
    override val maker: String,
    override val taker: String? = null,
    override val collection: List<String>,
    override val itemId: List<String>,
) : ElasticActivity()

data class ElasticOrderCancelBidActivity(
    override val id: String,

    override val date: Instant,
    override val blockNumber: Long?,
    override val logIndex: Long?,

    override val blockchain: BlockchainDto,
    override val type: ActivityTypeDto = ActivityTypeDto.CANCEL_BID,
    override val maker: String,
    override val taker: String? = null,
    override val collection: List<String>,
    override val itemId: List<String>,
) : ElasticActivity()

data class ElasticOrderCancelListActivity(
    override val id: String,

    override val date: Instant,
    override val blockNumber: Long?,
    override val logIndex: Long?,

    override val blockchain: BlockchainDto,
    override val type: ActivityTypeDto = ActivityTypeDto.CANCEL_LIST,
    override val maker: String,
    override val taker: String? = null,
    override val collection: List<String>,
    override val itemId: List<String>,
) : ElasticActivity()

// Auction Activities

data class ElasticAuctionOpenActivity(
    override val id: String,

    override val date: Instant,
    override val blockNumber: Long?,
    override val logIndex: Long?,

    override val blockchain: BlockchainDto,
    override val type: ActivityTypeDto = ActivityTypeDto.AUCTION_CREATED,
    override val maker: String,
    override val taker: String? = null,
    override val collection: List<String>,
    override val itemId: List<String>,
) : ElasticActivity()

data class ElasticAuctionBidActivity(
    override val id: String,

    override val date: Instant,
    override val blockNumber: Long?,
    override val logIndex: Long?,

    override val blockchain: BlockchainDto,
    override val type: ActivityTypeDto = ActivityTypeDto.AUCTION_BID,
    override val maker: String,
    override val taker: String? = null,
    override val collection: List<String>,
    override val itemId: List<String>,
) : ElasticActivity()

data class ElasticAuctionFinishActivity(
    override val id: String,

    override val date: Instant,
    override val blockNumber: Long?,
    override val logIndex: Long?,

    override val blockchain: BlockchainDto,
    override val type: ActivityTypeDto = ActivityTypeDto.AUCTION_FINISHED,
    override val maker: String,
    override val taker: String? = null,
    override val collection: List<String>,
    override val itemId: List<String>,
) : ElasticActivity()

data class ElasticAuctionCancelActivity(
    override val id: String,

    override val date: Instant,
    override val blockNumber: Long?,
    override val logIndex: Long?,

    override val blockchain: BlockchainDto,
    override val type: ActivityTypeDto = ActivityTypeDto.AUCTION_CANCEL,
    override val maker: String,
    override val taker: String? = null,
    override val collection: List<String>,
    override val itemId: List<String>,
) : ElasticActivity()

data class ElasticAuctionStartActivity(
    override val id: String,

    override val date: Instant,
    override val blockNumber: Long?,
    override val logIndex: Long?,

    override val blockchain: BlockchainDto,
    override val type: ActivityTypeDto = ActivityTypeDto.AUCTION_STARTED,
    override val maker: String,
    override val taker: String? = null,
    override val collection: List<String>,
    override val itemId: List<String>,
) : ElasticActivity()

data class ElasticAuctionEndActivity(
    override val id: String,

    override val date: Instant,
    override val blockNumber: Long?,
    override val logIndex: Long?,

    override val blockchain: BlockchainDto,
    override val type: ActivityTypeDto = ActivityTypeDto.AUCTION_ENDED,
    override val maker: String,
    override val taker: String? = null,
    override val collection: List<String>,
    override val itemId: List<String>,
) : ElasticActivity()
