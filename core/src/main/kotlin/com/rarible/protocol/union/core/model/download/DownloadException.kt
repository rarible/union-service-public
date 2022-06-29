package com.rarible.protocol.union.core.model.download

// TODO we can extend it to pass reason of fail (like timeout/parsing error etc)
class DownloadException(message: String) : RuntimeException(message)