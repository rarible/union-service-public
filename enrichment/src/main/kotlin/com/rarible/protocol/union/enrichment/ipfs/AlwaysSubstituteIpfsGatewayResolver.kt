package com.rarible.protocol.union.enrichment.ipfs

import com.rarible.core.meta.resource.model.IpfsUrl
import com.rarible.core.meta.resource.resolver.CustomIpfsGatewayResolver

class AlwaysSubstituteIpfsGatewayResolver : CustomIpfsGatewayResolver {
    override fun getResourceUrl(ipfsUrl: IpfsUrl, gateway: String, replaceOriginalHost: Boolean): String? {
        return "$gateway/${IpfsUrl.IPFS}/${ipfsUrl.path}"
    }
}
