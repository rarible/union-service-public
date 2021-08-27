package com.rarible.protocol.union.api.controller

import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.UnionOrdersDto
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@RestController
class OrderController : OrderControllerApi {

    override suspend fun getOrdersAll(
        blockchains: List<BlockchainDto>?,
        origin: String?,
        continuation: String?,
        size: Int?
    ): ResponseEntity<UnionOrdersDto> {
        TODO("Not yet implemented")
    }

    override suspend fun getOrderByHashOrId(
        hashOrId: String
    ): ResponseEntity<UnionOrdersDto> {
        TODO("Not yet implemented")
    }

    override suspend fun updateOrderMakeStock(
        hashOrId: String
    ): ResponseEntity<UnionOrdersDto> {
        TODO("Not yet implemented")
    }

    override suspend fun getOrderBidsByItem(
        contract: String,
        tokenId: String,
        maker: String?,
        origin: String?,
        continuation: String?,
        size: Int?
    ): ResponseEntity<UnionOrdersDto> {
        TODO("Not yet implemented")
    }

    override suspend fun getOrderBidsByMaker(
        maker: String,
        origin: String?,
        continuation: String?,
        size: Int?
    ): ResponseEntity<UnionOrdersDto> {
        TODO("Not yet implemented")
    }

    override suspend fun getSellOrders(
        origin: String?,
        continuation: String?,
        size: Int?
    ): ResponseEntity<UnionOrdersDto> {
        TODO("Not yet implemented")
    }

    override suspend fun getSellOrdersByCollection(
        collection: String,
        origin: String?,
        continuation: String?,
        size: Int?
    ): ResponseEntity<UnionOrdersDto> {
        TODO("Not yet implemented")
    }

    override suspend fun getSellOrdersByItem(
        contract: String,
        tokenId: String,
        maker: String?,
        origin: String?,
        continuation: String?,
        size: Int?
    ): ResponseEntity<UnionOrdersDto> {
        TODO("Not yet implemented")
    }

    override suspend fun getSellOrdersByMaker(
        maker: String,
        origin: String?,
        continuation: String?,
        size: Int?
    ): ResponseEntity<UnionOrdersDto> {
        TODO("Not yet implemented")
    }
}