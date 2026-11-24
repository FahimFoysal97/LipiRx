package com.snehonir.lipirx.domain.common

fun interface LipiRxUseCase<in Params, out Output> {
    operator fun invoke(params: Params): Output
}

fun interface LipiRxSuspendUseCase<in Params, out Output> {
    suspend operator fun invoke(params: Params): Result<Output>
}

object LipiRxNoParams
