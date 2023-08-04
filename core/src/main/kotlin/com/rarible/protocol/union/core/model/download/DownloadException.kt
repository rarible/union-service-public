package com.rarible.protocol.union.core.model.download

import com.rarible.protocol.union.core.model.ContentOwner

// TODO we can extend it to pass reason of fail (like timeout/parsing error etc)
class DownloadException(message: String) : RuntimeException(message)

class ProviderDownloadException(val provider: MetaSource) :
    RuntimeException("Failed to download meta from provider: $provider")

class PartialDownloadException(
    val failedProviders: List<MetaSource>,
    val data: ContentOwner<*>
) : RuntimeException("Failed to download meta from providers: $failedProviders")
