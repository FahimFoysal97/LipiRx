package com.snehonir.lipirx.presentation.preview

import com.snehonir.lipirx.domain.common.LipiRxNoParams
import com.snehonir.lipirx.domain.session.PrescriptionSessionState
import com.snehonir.lipirx.domain.usecase.GetAgentConfigsUseCase
import com.snehonir.lipirx.domain.usecase.LaunchPipelineParams
import com.snehonir.lipirx.domain.usecase.LaunchPrescriptionPipelineUseCase
import com.snehonir.lipirx.presentation.common.LipiRxViewModel
import com.snehonir.lipirx.resources.Res
import com.snehonir.lipirx.resources.error_please_select_image
import kotlinx.collections.immutable.toImmutableMap
import org.jetbrains.compose.resources.getString

class ImagePreviewViewModel(
    private val sessionState: PrescriptionSessionState,
    private val getAgentConfigsUseCase: GetAgentConfigsUseCase,
    private val launchPrescriptionPipelineUseCase: LaunchPrescriptionPipelineUseCase
) : LipiRxViewModel<ImagePreviewUiState>(ImagePreviewUiState()) {

    init {
        launchSafe {
            sessionState.session.collect { data ->
                val configs = getAgentConfigsUseCase(LipiRxNoParams).getOrDefault(emptyMap()).toImmutableMap()

                updateState {
                    copy(
                        imageByteArray = data.imageByteArray,
                        agentConfigs = configs,
                        errorMessage = data.errorMessage,
                    )
                }
            }
        }
    }

    fun refreshAgentConfigs() {
        launchSafe {
            val configs = getAgentConfigsUseCase(LipiRxNoParams).getOrDefault(emptyMap()).toImmutableMap()

            updateState {
                copy(agentConfigs = configs)
            }
        }
    }

    fun onImageSelected(bytes: ByteArray) {
        sessionState.setImageBytes(bytes)
    }

    fun clearSelectedImage() {
        sessionState.setImageBytes(null)
    }

    fun processSelectedImage(
        flow: String = "App -> $TAG.processSelectedImage",
        onStarted: (Boolean) -> Unit = {}
    ) {
        val bytes = sessionState.session.value.imageByteArray
        if (bytes == null || bytes.isEmpty()) {
            launchSafe {
                val err = getString(Res.string.error_please_select_image)
                updateState { copy(errorMessage = err) }
                onStarted(false)
            }
            return
        }

        launchSafe {
            val currentAgentConfigs = getAgentConfigsUseCase(LipiRxNoParams).getOrDefault(emptyMap()).toImmutableMap()
            updateState { copy(agentConfigs = currentAgentConfigs) }

            launchPrescriptionPipelineUseCase(
                LaunchPipelineParams(
                    imageByteArray = bytes,
                    flow = flow,
                    tag = TAG ?: "ImagePreviewViewModel",
                    onStarted = onStarted
                )
            )
        }
    }

    companion object {
        private val TAG = ImagePreviewViewModel::class.simpleName
    }
}
