package com.martmists.kmine.network.payloads

import kotlinx.serialization.Serializable

@Serializable
data class VersionInfo(
    val name: String,
    val protocol: Int
)

@Serializable
data class PlayersInfo(
    val max: Int,
    val online: Int,
    val sample: List<PlayerInfo>
)

@Serializable
data class PlayerInfo(
    val name: String,
    val id: String
)

@Serializable
data class DescriptionInfo(
    val text: String
)

@Serializable
data class StatusResponse(
    val version: VersionInfo,
    val players: PlayersInfo,
    val description: DescriptionInfo,
    val favicon: String? = null
)
