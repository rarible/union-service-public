package com.rarible.protocol.union.core.util

import com.rarible.core.meta.resource.model.MimeType
import org.apache.tika.mime.MimeTypeException
import org.apache.tika.mime.MimeTypes

object ExtensionParser {

    private val allTypes = MimeTypes.getDefaultMimeTypes()

    // File extensions for specific mime-types, ideally should be added to Tika XML
    private val dedicated = mapOf(
        MimeType.GLTF_JSON_MODEL.value to ".gltf",
        MimeType.GLTF_BINARY_MODEL.value to ".glb",

        MimeType.APNG_IMAGE.value to ".apng",

        MimeType.MP3_AUDIO.value to ".mp3",
        MimeType.WAV_AUDIO.value to ".wav",
        MimeType.FLAC_AUDIO.value to ".flac"
    )

    fun getFileExtension(mimeType: String): String? {
        dedicated[mimeType]?.let { return it }

        return try {
            allTypes.forName(mimeType).extension
        } catch (e: MimeTypeException) {
            null
        }
    }
}
