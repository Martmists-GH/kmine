package com.martmists.kmine.network.payloads

import kotlinx.serialization.Serializable

@Serializable
data class UserProperties(
    val name: String,
    val value: String,
    val signature: String
)

@Serializable
data class UserPayload(
    val id: String,
    val name: String,
    val properties: List<UserProperties>
)
