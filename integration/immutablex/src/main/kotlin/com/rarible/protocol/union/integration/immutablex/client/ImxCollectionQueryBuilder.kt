package com.rarible.protocol.union.integration.immutablex.client

import org.apache.commons.codec.binary.Base64
import org.springframework.web.util.UriBuilder

class ImxCollectionQueryBuilder(
    builder: UriBuilder
) : AbstractImxQueryBuilder(
    builder, PATH
) {

    companion object {

        const val PATH = "/collections"

        fun getByIdPath(collection: String): String {
            return "$PATH/${collection}"
        }
    }

    fun continuation(continuation: String?) {
        // Hack for IMX cursor - they use base64 of JSON of the last entity at the page,
        // but we can pass here only one field to make cursor acceptable - address
        val cursor = continuation
            ?.let { """{"address":"$continuation"}""" }
            ?.let { Base64.encodeBase64String(it.toByteArray()) }
            ?.trimEnd('=')


        builder.queryParamNotNull("cursor", cursor)
        orderBy("address", "asc")
    }

}