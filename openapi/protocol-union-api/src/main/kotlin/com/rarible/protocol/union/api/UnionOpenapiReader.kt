package com.rarible.protocol.union.api

import java.io.InputStream

object UnionOpenapiReader {

    fun getOpenapi(): InputStream {
        return UnionOpenapiReader::class.java.getResourceAsStream("/union.yaml")
    }

}