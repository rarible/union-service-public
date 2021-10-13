package com.rarible.protocol.union.enrichment.meta

import com.rarible.protocol.union.enrichment.configuration.MetaProperties
import org.springframework.stereotype.Service

@Service
class IpfsService(
    private val ipfsProperties: MetaProperties
) {
    fun resolveRealUrl(uri: String): String {
        val ipfsUri = if (uri.contains("/ipfs/")) {
            "ipfs:/${uri.substringBeforeLast("/ipfs/")}"
        } else {
            uri
        }
        val gateway = ipfsProperties.ipfsGateway
        return when {
            ipfsUri.startsWith("http") -> ipfsUri
            ipfsUri.startsWith("ipfs:///ipfs/") -> "$gateway/ipfs/${ipfsUri.removePrefix("ipfs:///ipfs/")}"
            ipfsUri.startsWith("ipfs://ipfs/") -> "$gateway/ipfs/${ipfsUri.removePrefix("ipfs://ipfs/")}"
            ipfsUri.startsWith("ipfs://") -> "$gateway/ipfs/${ipfsUri.removePrefix("ipfs://")}"
            ipfsUri.startsWith("Qm") -> "$gateway/ipfs/$ipfsUri"
            else -> gateway.trimEnd('/') + '/' + ipfsUri.trimStart('/')
        }
    }
}
