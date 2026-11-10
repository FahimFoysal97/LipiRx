package com.snehonir.lipirx.domain.agent

interface RxAgent<Input, Output> {
    val agentName: String
    val description: String

    suspend fun execute(input: Input): Result<Output>
}
