package com.rarible.protocol.union.api.client

import java.net.URI

class FixedUnionApiServiceUriProvider(private val fixedURI: URI) : UnionApiServiceUriProvider {

    override fun getUri(): URI {
        return fixedURI
    }
}