package com.rarible.protocol.union.core.restriction

data class RestrictionCheckResult(
    val success: Boolean,
    val message: String? = null
) {
    fun reduce(other: RestrictionCheckResult): RestrictionCheckResult {
        val combinedMessage = when {
            this.message != null && other.message != null -> "${this.message}; ${other.message}"
            else -> this.message ?: other.message
        }
        return RestrictionCheckResult(
            success = this.success && other.success,
            message = combinedMessage
        )
    }
}