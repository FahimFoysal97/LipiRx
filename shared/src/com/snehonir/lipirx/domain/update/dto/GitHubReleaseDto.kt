package com.snehonir.lipirx.domain.update.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GitHubReleaseDto(
    @SerialName("tag_name") val tagName: String? = null,
    val name: String? = null,
    val body: String? = null,
    val assets: List<GitHubAssetDto>? = null
)

@Serializable
data class GitHubAssetDto(
    val name: String? = null,
    @SerialName("browser_download_url") val browserDownloadUrl: String? = null
)
