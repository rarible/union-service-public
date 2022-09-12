package com.rarible.protocol.union.api.controller

import com.rarible.protocol.union.core.exception.UnionNotFoundException
import com.rarible.protocol.union.core.util.ExtensionParser
import com.rarible.protocol.union.enrichment.meta.embedded.EmbeddedContentService
import org.slf4j.LoggerFactory
import org.springframework.http.ContentDisposition
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController

@RestController
class ContentController(
    private val embeddedContentService: EmbeddedContentService
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @GetMapping(value = ["/content/embedded/{id}"])
    suspend fun getEmbeddedMedia(
        @PathVariable("id") id: String
    ): ResponseEntity<Any> {
        val content = embeddedContentService.get(id)
            ?: throw UnionNotFoundException("Embedded content with ID $id not found")

        val fileExtension = ExtensionParser.getFileExtension(content.mimeType)
        if (fileExtension == null) {
            logger.warn("Can't get file extension for mimeType={}", content.mimeType)
        }

        val filename = fileExtension?.let { "$id$fileExtension" } ?: id
        val contentDisposition = ContentDisposition.attachment().filename(filename).build()

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_TYPE, content.mimeType)
            .header(HttpHeaders.CONTENT_LENGTH, content.size.toString())
            .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
            .body(content.data)
    }
}