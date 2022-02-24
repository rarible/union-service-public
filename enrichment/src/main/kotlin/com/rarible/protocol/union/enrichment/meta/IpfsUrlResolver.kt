package com.rarible.protocol.union.enrichment.meta

import com.rarible.protocol.union.enrichment.configuration.UnionMetaProperties
import org.springframework.stereotype.Component

@Component
class IpfsUrlResolver(
    ipfsProperties: UnionMetaProperties
) {

    private val gateway = ipfsProperties.ipfsGateway.trimEnd('/')

    fun resolveIpfsUrl(uri: String): String =
        if (uri.contains("/ipfs/")) {
            "ipfs:/${uri.substring(uri.lastIndexOf("/ipfs/"))}"
        } else {
            uri
        }

    fun resolveRealUrl(uri: String): String {
        val ipfsUri = resolveIpfsUrl(uri)
        return when {
            ipfsUri.startsWith("http") -> ipfsUri
            ipfsUri.startsWith("ipfs:///ipfs/") -> "$gateway/ipfs/${ipfsUri.removePrefix("ipfs:///ipfs/")}"
            ipfsUri.startsWith("ipfs://ipfs/") -> "$gateway/ipfs/${ipfsUri.removePrefix("ipfs://ipfs/")}"
            ipfsUri.startsWith("ipfs://") -> "$gateway/ipfs/${ipfsUri.removePrefix("ipfs://")}"
            ipfsUri.startsWith("Qm") -> "$gateway/ipfs/$ipfsUri"
            else -> "$gateway/${ipfsUri.trimStart('/')}"
        }
    }
}
