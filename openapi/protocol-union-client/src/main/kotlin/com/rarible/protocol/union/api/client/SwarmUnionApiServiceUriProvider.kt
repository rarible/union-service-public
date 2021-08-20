package com.rarible.protocol.union.api.client

import java.net.URI

class SwarmUnionApiServiceUriProvider(
    private val environment: String
) : UnionApiServiceUriProvider {

    override fun getUri(): URI {
        return URI.create(String.format("http://%s-union-api:8080", environment))
    }
}