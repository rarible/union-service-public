package com.rarible.protocol.union.api.controller

import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.UnionItemDto
import com.rarible.protocol.union.dto.UnionItemsDto
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@RestController
class ItemController : ItemControllerApi {

    override suspend fun getAllItems(
        blockchains: List<BlockchainDto>?,
        continuation: String?,
        size: Int?,
        showDeleted: Boolean?,
        lastUpdatedFrom: Long?,
        lastUpdatedTo: Long?,
        includeMeta: Boolean?
    ): ResponseEntity<UnionItemsDto> {
        TODO("Not yet implemented")
    }

    override suspend fun getItemById(
        itemId: String,
        includeMeta: Boolean?
    ): ResponseEntity<UnionItemDto> {
        TODO("Not yet implemented")
    }

    override suspend fun getItemsByCollection(
        collection: String,
        continuation: String?,
        size: Int?,
        includeMeta: Boolean?
    ): ResponseEntity<UnionItemsDto> {
        TODO("Not yet implemented")
    }

    override suspend fun getItemsByCreator(
        creator: String,
        continuation: String?,
        size: Int?,
        includeMeta: Boolean?
    ): ResponseEntity<UnionItemsDto> {
        TODO("Not yet implemented")
    }

    override suspend fun getItemsByOwner(
        owner: String,
        continuation: String?,
        size: Int?,
        includeMeta: Boolean?
    ): ResponseEntity<UnionItemsDto> {
        TODO("Not yet implemented")
    }

}