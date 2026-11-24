package com.snehonir.lipirx.presentation.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

abstract class LipiRxViewModel<State>(
    initialState: State
) : ViewModel() {

    private val _uiState = MutableStateFlow(initialState)
    val uiState: StateFlow<State> = _uiState.asStateFlow()

    protected val currentState: State
        get() = _uiState.value

    protected fun updateState(reducer: State.() -> State) {
        _uiState.update(reducer)
    }

    protected fun launchSafe(
        dispatcher: CoroutineDispatcher = Dispatchers.Default,
        onError: ((Throwable) -> Unit)? = null,
        block: suspend CoroutineScope.() -> Unit
    ): Job {
        return viewModelScope.launch(dispatcher) {
            runCatching { block() }
                .onFailure { throwable ->
                    if (throwable is CancellationException) throw throwable
                    Napier.e("Exception in ${this@LipiRxViewModel::class.simpleName}: ${throwable.message}", throwable)
                    onError?.invoke(throwable)
                }
        }
    }
}
