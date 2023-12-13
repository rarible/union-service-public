package com.rarible.protocol.union.api.controller

import com.rarible.protocol.union.api.service.elastic.LegacyItemTraitService
import com.rarible.protocol.union.api.service.select.ActivitySourceSelectService
import com.rarible.protocol.union.api.service.select.CollectionSourceSelectService
import com.rarible.protocol.union.api.service.select.ItemSourceSelectService
import com.rarible.protocol.union.api.service.select.OwnershipSourceSelectService
import com.rarible.protocol.union.dto.ActivitiesDto
import com.rarible.protocol.union.dto.ActivitySearchRequestDto
import com.rarible.protocol.union.dto.ActivitySearchSortDto
import com.rarible.protocol.union.dto.ActivitySortDto
import com.rarible.protocol.union.dto.CollectionsDto
import com.rarible.protocol.union.dto.CollectionsSearchRequestDto
import com.rarible.protocol.union.dto.ItemsDto
import com.rarible.protocol.union.dto.ItemsSearchRequestDto
import com.rarible.protocol.union.dto.OwnershipSearchRequestDto
import com.rarible.protocol.union.dto.OwnershipsDto
import com.rarible.protocol.union.dto.TraitsDto
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class SearchController(
    private val activitySourceSelector: ActivitySourceSelectService,
    private val collectionSourceSelector: CollectionSourceSelectService,
    private val itemSourceSelectService: ItemSourceSelectService,
    private val itemTraitService: LegacyItemTraitService,
    private val ownershipSourceSelectService: OwnershipSourceSelectService,
) : SearchControllerApi {
    override suspend fun searchActivities(
        activitySearchRequestDto: ActivitySearchRequestDto
    ): ResponseEntity<ActivitiesDto> {
        val sort = when (activitySearchRequestDto.sort) {
            ActivitySearchSortDto.EARLIEST -> ActivitySortDto.EARLIEST_FIRST
            ActivitySearchSortDto.LATEST -> ActivitySortDto.LATEST_FIRST
            else -> null
        }
        val result = activitySourceSelector.search(
            filter = activitySearchRequestDto.filter,
            cursor = activitySearchRequestDto.cursor,
            size = activitySearchRequestDto.size,
            sort = sort
        )
        return ResponseEntity.ok(result)
    }

    override suspend fun searchCollection(
        collectionsSearchRequestDto: CollectionsSearchRequestDto
    ): ResponseEntity<CollectionsDto> {
        return ResponseEntity.ok(collectionSourceSelector.searchCollections(collectionsSearchRequestDto))
    }

    override suspend fun searchItems(itemsSearchRequestDto: ItemsSearchRequestDto): ResponseEntity<ItemsDto> {
        logger.info("Got request to search items: $itemsSearchRequestDto")
        return ResponseEntity.ok(itemSourceSelectService.searchItems(itemsSearchRequestDto))
    }

    override suspend fun searchOwnerships(
        ownershipSearchRequestDto: OwnershipSearchRequestDto
    ): ResponseEntity<OwnershipsDto> {
        return ResponseEntity.ok(ownershipSourceSelectService.search(ownershipSearchRequestDto))
    }

    override suspend fun searchTraits(
        @RequestParam filter: String,
        @RequestParam collectionIds: List<String>
    ): ResponseEntity<TraitsDto> {
        return ResponseEntity.ok(
            itemTraitService.searchTraits(
                filter = filter,
                collectionIds = collectionIds
            )
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(SearchController::class.java)
    }
}
