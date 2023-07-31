package com.rarible.protocol.union.core.test

import com.rarible.core.test.wait.Wait
import java.time.Duration

object WaitAssert {

    suspend fun wait(
        checkInterval: Duration = Duration.ofMillis(100),
        timeout: Duration = Duration.ofSeconds(10),
        runnable: suspend () -> Unit
    ) {
        Wait.waitAssertWithCheckInterval(checkInterval, timeout, runnable)
    }
}
