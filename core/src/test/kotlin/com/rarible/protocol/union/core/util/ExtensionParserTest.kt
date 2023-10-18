package com.rarible.protocol.union.core.util

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ExtensionParserTest {

    @Test
    fun `get file extension`() {
        assertMimeType("text/html", ".html")

        assertMimeType("image/png", ".png")
        assertMimeType("image/apng", ".apng")
        assertMimeType("image/svg+xml", ".svg")
        assertMimeType("image/jpeg", ".jpg")
        assertMimeType("image/gif", ".gif")
        assertMimeType("image/bmp", ".bmp")
        assertMimeType("image/webp", ".webp")

        assertMimeType("video/mp4", ".mp4")
        assertMimeType("video/webm", ".webm")
        assertMimeType("video/x-msvideo", ".avi")
        assertMimeType("video/mpeg", ".mpeg")

        assertMimeType("audio/mp3", ".mp3")
        assertMimeType("audio/wav", ".wav")
        assertMimeType("audio/flac", ".flac")
        assertMimeType("audio/mpeg", ".mpga")

        assertMimeType("model/gltf+json", ".gltf")
        assertMimeType("model/gltf-binary", ".glb")
    }

    private fun assertMimeType(mimeType: String, expected: String) {
        assertThat(ExtensionParser.getFileExtension(mimeType)).isEqualTo(expected)
    }
}
