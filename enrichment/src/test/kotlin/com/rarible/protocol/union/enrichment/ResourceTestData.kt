package com.rarible.protocol.union.enrichment

import com.rarible.core.meta.resource.ArweaveUrl
import com.rarible.core.meta.resource.ConstantGatewayProvider
import com.rarible.core.meta.resource.LegacyIpfsGatewaySubstitutor
import com.rarible.core.meta.resource.RandomGatewayProvider
import com.rarible.core.meta.resource.cid.CidV1Validator
import com.rarible.core.meta.resource.parser.ArweaveUrlResourceParser
import com.rarible.core.meta.resource.parser.CidUrlResourceParser
import com.rarible.core.meta.resource.parser.DefaultUrlResourceParserProvider
import com.rarible.core.meta.resource.parser.HttpUrlResourceParser
import com.rarible.core.meta.resource.parser.UrlResourceParsingProcessor
import com.rarible.core.meta.resource.parser.ipfs.AbstractIpfsUrlResourceParser
import com.rarible.core.meta.resource.parser.ipfs.ForeignIpfsUrlResourceParser
import com.rarible.core.meta.resource.resolver.ArweaveGatewayResolver
import com.rarible.core.meta.resource.resolver.IpfsCidGatewayResolver
import com.rarible.core.meta.resource.resolver.IpfsGatewayResolver
import com.rarible.core.meta.resource.resolver.SimpleHttpGatewayResolver
import com.rarible.core.meta.resource.resolver.UrlResolver
import com.rarible.protocol.union.enrichment.meta.UrlService

object ResourceTestData {
    const val IPFS_PUBLIC_GATEWAY = "https://ipfs.io"
    const val IPFS_PRIVATE_GATEWAY = "https://ipfs_private.io"
    const val IPFS_CUSTOM_GATEWAY = "https://rarible.mypinata.com" // Legacy
    const val CID = "QmbpJhWFiwzNu7MebvKG3hrYiyWmSiz5dTUYMQLXsjT9vw"
    const val ITEM_ID = "itemId"

    private val cidOneValidator = CidV1Validator()
    private val foreignIpfsUrlResourceParser = ForeignIpfsUrlResourceParser(
        cidOneValidator = cidOneValidator
    )

    private val ipfsGatewayResolver = IpfsGatewayResolver(
        publicGatewayProvider = ConstantGatewayProvider(IPFS_PUBLIC_GATEWAY),
        innerGatewaysProvider = RandomGatewayProvider(listOf(IPFS_PRIVATE_GATEWAY)),
        customGatewaysResolver = LegacyIpfsGatewaySubstitutor(listOf(IPFS_CUSTOM_GATEWAY))
    )

    private val defaultUrlResourceParserProvider = DefaultUrlResourceParserProvider(
        arweaveUrlParser = ArweaveUrlResourceParser(),
        foreignIpfsUrlResourceParser = foreignIpfsUrlResourceParser,
        abstractIpfsUrlResourceParser = AbstractIpfsUrlResourceParser(),
        cidUrlResourceParser = CidUrlResourceParser(cidOneValidator),
        httpUrlParser = HttpUrlResourceParser()
    )

    private val urlResourceParsingProcessor = UrlResourceParsingProcessor(
        provider = defaultUrlResourceParserProvider
    )

    private val urlResolver = UrlResolver(
        ipfsGatewayResolver = ipfsGatewayResolver,
        ipfsCidGatewayResolver = IpfsCidGatewayResolver(
            publicGatewayProvider = ConstantGatewayProvider(IPFS_PUBLIC_GATEWAY),
            innerGatewaysProvider = RandomGatewayProvider(listOf(IPFS_PUBLIC_GATEWAY))
        ),
        arweaveGatewayResolver = ArweaveGatewayResolver(
            arweaveGatewayProvider = ConstantGatewayProvider(ArweaveUrl.ARWEAVE_GATEWAY)
        ),
        simpleHttpGatewayResolver = SimpleHttpGatewayResolver()
    )

    val urlService = UrlService(
        urlResourceProcessor = urlResourceParsingProcessor,
        urlResolver = urlResolver
    )
}
