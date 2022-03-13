package com.rarible.protocol.union.enrichment.meta

import com.rarible.protocol.union.enrichment.configuration.UnionMetaProperties
import org.springframework.stereotype.Component
import java.util.regex.Pattern

@Component
class IpfsUrlResolver(
    ipfsProperties: UnionMetaProperties
) {

    private val gateway = ipfsProperties.ipfsGateway.trimEnd('/')

    fun resolveRealUrl(uri: String): String {
        val ipfsUri = if (uri.contains("/ipfs/")) {
            val ipfsHash = uri.substringAfterLast("/ipfs/")
            if (isCid(ipfsHash.substringBefore("/"))) {
                "ipfs://$ipfsHash"
            } else {
                uri
            }
        } else {
            uri
        }
        return when {
            ipfsUri.startsWith("http") -> ipfsUri
            ipfsUri.startsWith("ipfs:///ipfs/") -> "$gateway/ipfs/${ipfsUri.removePrefix("ipfs:///ipfs/")}"
            ipfsUri.startsWith("ipfs://ipfs/") -> "$gateway/ipfs/${ipfsUri.removePrefix("ipfs://ipfs/")}"
            ipfsUri.startsWith("ipfs://") -> "$gateway/ipfs/${ipfsUri.removePrefix("ipfs://")}"
            ipfsUri.startsWith("Qm") -> "$gateway/ipfs/$ipfsUri"
            else -> "$gateway/${ipfsUri.trimStart('/')}"
        }.encodeHtmlUrl()
    }

    private fun String.encodeHtmlUrl(): String = replace(" ", "%20")

    fun isCid(test: String): Boolean = CID_PATTERN.matcher(test).matches()

    companion object {
        private val CID_PATTERN = Pattern.compile(
            "Qm[1-9A-HJ-NP-Za-km-z]{44,}|b[A-Za-z2-7]{58,}|B[A-Z2-7]{58,}|z[1-9A-HJ-NP-Za-km-z]{48,}|F[0-9A-F]{50,}|f[0-9a-f]{50,}"
        )
    }
}
