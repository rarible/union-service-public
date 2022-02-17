package com.rarible.protocol.union.enrichment.meta

import com.google.common.io.ByteStreams
import com.google.common.io.CountingInputStream
import com.google.common.net.InternetDomainName
import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.core.cache.CacheDescriptor
import com.rarible.core.client.WebClientHelper
import com.rarible.core.common.blockingToMono
import com.rarible.core.common.nowMillis
import com.rarible.core.logging.LoggingUtils
import com.rarible.protocol.union.enrichment.configuration.MetaProperties
import com.rarible.protocol.union.enrichment.util.spent
import com.sun.imageio.plugins.bmp.BMPMetadata
import com.sun.imageio.plugins.gif.GIFImageMetadata
import com.sun.imageio.plugins.jpeg.JPEGMetadata
import com.sun.imageio.plugins.png.PNGMetadata
import org.apache.commons.lang3.time.DateUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty
import reactor.kotlin.core.publisher.toMono
import java.io.FilterInputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.URI
import java.net.URL
import java.net.URLConnection
import java.util.concurrent.Callable
import javax.imageio.ImageIO
import javax.imageio.metadata.IIOMetadata

@Component
@Suppress("UnstableApiUsage")
class MediaMetaService(
    private val metaProperties: MetaProperties
) : CacheDescriptor<ContentMeta> {

    private val client = WebClient.builder()
        .clientConnector(
            WebClientHelper.createConnector(
                metaProperties.mediaFetchTimeout,
                metaProperties.mediaFetchTimeout,
                true
            )
        )
        .build()

    override val collection: String = "cache_meta"

    override fun getMaxAge(value: ContentMeta?): Long =
        if (value == null) {
            DateUtils.MILLIS_PER_HOUR
        } else {
            Long.MAX_VALUE
        }

    @CaptureSpan(type = SpanType.EXT)
    override fun get(url: String): Mono<ContentMeta> {
        val now = nowMillis()
        val result = LoggingUtils.withMarker { marker ->
            logger.info(marker, "Fetching meta by URL: {}", url)
            when {
                url.endsWith(".mp4") -> ContentMeta("video/mp4").toMono()
                url.endsWith(".webm") -> ContentMeta("video/webm").toMono()
                url.endsWith(".mp3") -> ContentMeta("audio/mp3").toMono()
                url.endsWith(".mpga") -> ContentMeta("audio/mpeg").toMono()
                url.endsWith(".wav") -> ContentMeta("audio/wav").toMono()
                url.endsWith(".flac") -> ContentMeta("audio/flac").toMono()
                url.endsWith(".svg") -> ContentMeta("image/svg+xml", 192, 192).toMono()
                else -> {
                    getMetadata(url)
                        .flatMap { (width, height, metadata, contentLength) ->
                            when (metadata) {
                                is GIFImageMetadata -> ContentMeta(
                                    type = "image/gif",
                                    width = metadata.imageWidth,
                                    height = metadata.imageHeight,
                                    size = contentLength
                                ).toMono()
                                is JPEGMetadata -> ContentMeta(
                                    type = "image/jpeg",
                                    width = width,
                                    height = height,
                                    size = contentLength
                                ).toMono()
                                is BMPMetadata -> ContentMeta(
                                    type = "image/bmp",
                                    width = width,
                                    height = height,
                                    size = contentLength
                                ).toMono()
                                is PNGMetadata -> ContentMeta(
                                    type = "image/png",
                                    width = width,
                                    height = height,
                                    size = contentLength
                                ).toMono()
                                else -> Mono.empty()
                            }
                        }
                        .switchIfEmpty {
                            when {
                                url.endsWith(".gif") -> ContentMeta("image/gif").toMono()
                                url.endsWith(".jpg") -> ContentMeta("image/jpeg").toMono()
                                url.endsWith(".jpeg") -> ContentMeta("image/jpeg").toMono()
                                url.endsWith(".png") -> ContentMeta("image/png").toMono()
                                else -> getMimeType(url)
                                    .map { ContentMeta(it) }
                            }
                        }
                        .switchIfEmpty {
                            logger.warn(marker, "Unable to get metadata for $url")
                            Mono.empty()
                        }
                }
            }
        }
        logger.info("Fetched meta by URL {} ({}ms)", url, spent(now))
        return result
    }

    private fun getMimeType(url: String): Mono<String> {
        return client.head()
            .uri(URI(url))
            .exchange()
            .flatMap {
                val type = it.headers().contentType()
                if (type.isPresent) {
                    type.get().toString().toMono()
                } else {
                    Mono.empty()
                }
            }
            .onErrorResume { Mono.empty() }
    }

    private data class RawMetadata(
        val width: Int,
        val height: Int,
        val metadata: IIOMetadata,
        val contentLength: Long?
    )

    private fun getMetadata(url: String): Mono<RawMetadata> {
        return Callable {
            val conn = connection(url) as HttpURLConnection
            conn.readTimeout = metaProperties.mediaFetchTimeout
            conn.connectTimeout = metaProperties.mediaFetchTimeout
            conn.setRequestProperty("user-agent", "curl/7.73.0")
            conn.inputStream.limited(url).use { getMetadata(conn, it) }
        }.blockingToMono()
            .flatMap { it?.toMono() ?: Mono.empty() }
            .onErrorResume { Mono.empty() }
    }


    private fun InputStream.limited(url: String): InputStream {
        val limitedStream = ByteStreams.limit(this, metaProperties.mediaFetchMaxSize)
        val countingStream = CountingInputStream(limitedStream)
        return object : FilterInputStream(countingStream) {
            override fun close() {
                if (countingStream.count > metaProperties.mediaFetchMaxSize / 2) {
                    logger.warn("Suspiciously many bytes (${countingStream.count}) are read from the content input stream for $url")
                }
                super.close()
            }
        }
    }

    private fun getMetadata(connection: HttpURLConnection, ins: InputStream): RawMetadata? {
        return ImageIO.createImageInputStream(ins).use { iis ->
            val readers = ImageIO.getImageReaders(iis)
            if (readers.hasNext()) {
                val r = readers.next()
                r.setInput(iis, true)
                try {
                    val contentLength = connection.contentLength.toLong().takeIf { it > 0 }
                        ?: connection.contentLengthLong.takeIf { it > 0 }
                        ?: iis.length().takeIf { it > 0 }
                    RawMetadata(
                        width = r.getWidth(0),
                        height = r.getHeight(0),
                        metadata = r.getImageMetadata(0),
                        contentLength = contentLength
                    )
                } finally {
                    r.dispose()
                }
            } else {
                return null
            }
        }
    }

    private fun connection(url: String): URLConnection {
        return when {
            isOpenSea(url) && metaProperties.openSeaProxyUrl.isNotEmpty() -> {
                val address = URL(metaProperties.openSeaProxyUrl)
                val proxy = Proxy(Proxy.Type.HTTP, InetSocketAddress(address.host, address.port))
                URL(url).openConnection(proxy)
            }
            else -> URL(url).openConnection()
        }
    }

    private fun isOpenSea(url: String): Boolean {
        val domain = InternetDomainName.from(URL(url).host).topPrivateDomain().toString()
        return domain.startsWith(OPENSEA_DOMAIN)
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(MediaMetaService::class.java)
        const val OPENSEA_DOMAIN = "opensea.io"
    }
}
