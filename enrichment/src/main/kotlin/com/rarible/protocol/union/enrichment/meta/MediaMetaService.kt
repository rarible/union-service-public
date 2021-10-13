package com.rarible.protocol.union.enrichment.meta

import com.google.common.net.InternetDomainName
import com.rarible.core.cache.CacheDescriptor
import com.rarible.core.client.WebClientHelper
import com.rarible.core.common.blockingToMono
import com.rarible.core.logging.LoggingUtils
import com.rarible.protocol.union.enrichment.configuration.MetaProperties
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
import reactor.kotlin.core.publisher.toMono
import java.io.IOException
import java.io.InputStream
import java.net.*
import java.util.concurrent.Callable
import javax.imageio.ImageIO
import javax.imageio.metadata.IIOMetadata

@Component
class MediaMetaService(
    private val metaProperties: MetaProperties
): CacheDescriptor<ContentMeta> {

    private val client = WebClient.builder()
        .clientConnector(WebClientHelper.createConnector(metaProperties.mediaFetchTimeout, metaProperties.mediaFetchTimeout, true))
        .build()

    override val collection: String = "cache_meta"

    override fun getMaxAge(value: ContentMeta?): Long =
        if (value == null) {
            DateUtils.MILLIS_PER_HOUR
        } else {
            Long.MAX_VALUE
        }

    override fun get(url: String): Mono<ContentMeta> {
        return LoggingUtils.withMarker { marker ->
            logger.info(marker, "getMediaMeta $url")
            when {
                url.endsWith(".mp4") ->
                    ContentMeta("video/mp4").toMono()
                url.endsWith(".webm") ->
                    ContentMeta("video/webm").toMono()
                url.endsWith(".mp3") ->
                    ContentMeta("audio/mp3").toMono()
                url.endsWith(".mpga") ->
                    ContentMeta("audio/mpeg").toMono()
                url.endsWith(".svg") ->
                    ContentMeta("image/svg+xml", 192, 192).toMono()
                else -> {
                    getMetadata(url)
                        .flatMap { (width, height, metadata) ->
                            when (metadata) {
                                is GIFImageMetadata -> ContentMeta("image/gif", metadata.imageWidth, metadata.imageHeight).toMono()
                                is JPEGMetadata -> ContentMeta("image/jpeg", width, height).toMono()
                                is BMPMetadata -> ContentMeta("image/bmp", width, height).toMono()
                                is PNGMetadata -> ContentMeta("image/png", width, height).toMono()
                                else -> Mono.error<ContentMeta>(IOException("Unknown metadata: " + metadata.javaClass.name))
                            }
                        }
                        .onErrorResume { ex ->
                            logger.warn(marker, "unable to get meta using image metadata", ex)
                            when {
                                url.endsWith(".gif") ->
                                    ContentMeta("image/gif").toMono()
                                url.endsWith(".jpg") ->
                                    ContentMeta("image/jpeg").toMono()
                                url.endsWith(".jpeg") ->
                                    ContentMeta("image/jpeg").toMono()
                                url.endsWith(".png") ->
                                    ContentMeta("image/png").toMono()
                                else -> getMimeType(url)
                                    .map { ContentMeta(it) }
                            }
                        }
                        .onErrorResume { ex ->
                            logger.warn(marker, "unable to get meta using HEAD request", ex)
                            Mono.empty()
                        }
                }
            }
        }
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
    }

    private fun getMetadata(url: String): Mono<Triple<Int, Int, IIOMetadata>> {
        return Callable {
            val conn = connection(url) as HttpURLConnection
            conn.readTimeout = metaProperties.mediaFetchTimeout
            conn.connectTimeout = metaProperties.mediaFetchTimeout
            conn.setRequestProperty("user-agent", "curl/7.73.0")
            conn.inputStream.use { get(it) }
        }.blockingToMono()
    }

    private fun get(ins: InputStream): Triple<Int, Int, IIOMetadata> {
        return ImageIO.createImageInputStream(ins).use { iis ->
            val readers = ImageIO.getImageReaders(iis)
            if (readers.hasNext()) {
                val r = readers.next()
                r.setInput(iis, true)
                try {
                    Triple(r.getWidth(0), r.getHeight(0), r.getImageMetadata(0))
                } finally {
                    r.dispose()
                }
            } else {
                throw IOException("reader not found")
            }
        }
    }

    private fun connection(url: String): URLConnection {
        return when {
            isOpenSea(url) -> {
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
