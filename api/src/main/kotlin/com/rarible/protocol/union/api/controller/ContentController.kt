package com.rarible.protocol.union.api.controller

import com.rarible.protocol.union.core.exception.UnionNotFoundException
import com.rarible.protocol.union.enrichment.meta.embedded.EmbeddedContentService
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController

@RestController
class ContentController(
    private val embeddedContentService: EmbeddedContentService
) {

    @GetMapping(value = ["/content/embedded/{id}"])
    suspend fun getEmbeddedMedia(
        @PathVariable("id") id: String
    ): ResponseEntity<Any> {
        val content = embeddedContentService.get(id)
            ?: throw UnionNotFoundException("Embedded content with ID $id not found")

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_TYPE, content.mimeType)
            .header(HttpHeaders.CONTENT_LENGTH, content.size.toString())
            .body(content.data)
    }

}