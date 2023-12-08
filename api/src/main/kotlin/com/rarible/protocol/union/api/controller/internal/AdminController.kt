package com.rarible.protocol.union.api.controller.internal

import com.rarible.protocol.union.api.service.api.CheapestOrderService
import com.rarible.protocol.union.api.service.api.CollectionApiService
import com.rarible.protocol.union.api.service.task.TaskSchedulingService
import com.rarible.protocol.union.core.service.OrderService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.core.task.Tasks
import com.rarible.protocol.union.dto.OrderDto
import com.rarible.protocol.union.dto.parser.IdParser
import com.rarible.protocol.union.enrichment.model.EnrichmentCollectionId
import com.rarible.protocol.union.enrichment.service.EnrichmentOrderService
import com.rarible.protocol.union.enrichment.service.TraitService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class AdminController(
    private val router: BlockchainRouter<OrderService>,
    private val enrichmentOrderService: EnrichmentOrderService,
    private val cheapestOrderService: CheapestOrderService,
    private val collectionApiService: CollectionApiService,
    private val traitService: TraitService,
    private val taskSchedulingService: TaskSchedulingService,
) {
    @PostMapping(
        value = ["/admin/orders/{orderId}/cancel"],
        produces = ["application/json"]
    )
    suspend fun cancelOrder(
        @PathVariable("orderId") orderId: String
    ): ResponseEntity<OrderDto> {
        val unionOrderId = IdParser.parseOrderId(orderId)
        val result = router.getService(unionOrderId.blockchain).cancelOrder(unionOrderId.value)
        return ResponseEntity.ok(enrichmentOrderService.enrich(result))
    }

    @GetMapping(
        value = ["/admin/collections/{collectionId}/cheapestOrder"],
        produces = ["application/json"]
    )
    suspend fun cheapestCollectionOrder(
        @PathVariable collectionId: String
    ): ResponseEntity<OrderDto> {
        val collectionIdDto = IdParser.parseCollectionId(collectionId)
        return ResponseEntity.ok(cheapestOrderService.getCheapestOrder(collectionIdDto))
    }

    @PostMapping(
        value = ["/admin/collections/{collectionId}/hasTraits"],
        produces = ["application/json"]
    )
    suspend fun updateHasTraits(
        @PathVariable collectionId: String,
        @RequestParam hasTraits: Boolean
    ): ResponseEntity<Unit> {
        val enrichmentCollectionId = EnrichmentCollectionId.of(collectionId)
        if (!collectionApiService.updateHasTraits(id = enrichmentCollectionId, hasTraits = hasTraits)) {
            return ResponseEntity.ok().build()
        }
        if (!hasTraits) {
            traitService.deleteAll(enrichmentCollectionId)
        } else {
            taskSchedulingService.schedule(Tasks.REFRESH_TRAITS_TASK, collectionId)
        }
        return ResponseEntity.ok().build()
    }
}
