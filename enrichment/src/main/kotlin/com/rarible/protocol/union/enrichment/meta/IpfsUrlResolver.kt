package com.rarible.protocol.union.enrichment.meta

import com.rarible.protocol.union.enrichment.configuration.UnionMetaProperties
import org.springframework.stereotype.Component
import java.util.regex.Pattern

@Component
class IpfsUrlResolver(
    ipfsProperties: UnionMetaProperties
) {

    private val innerGateway = ipfsProperties.ipfsGateway.trimEnd('/')
    private val publicGateway = ipfsProperties.ipfsPublicGateway.trimEnd('/')
    private val legacyGateway = ipfsProperties.ipfsLegacyGateway?.trimEnd('/')

    fun resolveRealUrl(uri: String): String {
        return resolveRealUrl(uri, innerGateway)
    }

    fun resolvePublicUrl(uri: String): String {
        return resolveRealUrl(uri, publicGateway)
    }

    private fun resolveRealUrl(uri: String, gateway: String): String {
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

        // Sometimes we have in mypinata urls not a CID-matched value, so here checking for legacy host
        if (legacyGateway != null && ipfsUri.startsWith(legacyGateway)) {
            return gateway + ipfsUri.substring(legacyGateway.length)
        }

        return when {
            ipfsUri.startsWith("http") -> ipfsUri
            ipfsUri.startsWith("ipfs:///ipfs/") -> "$gateway/ipfs/${ipfsUri.removePrefix("ipfs:///ipfs/")}"
            ipfsUri.startsWith("ipfs://ipfs/") -> "$gateway/ipfs/${ipfsUri.removePrefix("ipfs://ipfs/")}"
            ipfsUri.startsWith("ipfs://") -> "$gateway/ipfs/${ipfsUri.removePrefix("ipfs://")}"
            ipfsUri.startsWith("ipfs:/") -> "$gateway/ipfs/${ipfsUri.removePrefix("ipfs:/")}"
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
