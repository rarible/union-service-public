package com.rarible.protocol.union.meta.loader.executor

import com.rarible.protocol.union.core.model.download.DownloadEntry
import com.rarible.protocol.union.core.model.download.DownloadTask
import org.springframework.stereotype.Component

@Component
class DownloadDebouncer {

    fun debounce(task: DownloadTask, current: DownloadEntry<*>): Boolean {
        // TODO implement
        return false
    }

}