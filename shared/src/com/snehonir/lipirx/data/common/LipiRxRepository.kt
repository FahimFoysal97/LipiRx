package com.snehonir.lipirx.data.common

interface LipiRxRepository {
    suspend fun <T> safeDbCall(
        flow: String = "",
        block: suspend () -> T
    ): Result<T>
}
