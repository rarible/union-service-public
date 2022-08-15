package com.rarible.protocol.union.integration.immutablex.client

import org.apache.commons.codec.binary.Base64

object ImxCursor {

    fun encode(json: String): String {
        return Base64.encodeBase64String(json.toByteArray()).trimEnd('=')
    }

}