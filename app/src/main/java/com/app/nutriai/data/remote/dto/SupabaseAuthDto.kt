package com.app.nutriai.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ─────────────────────────────────────────────
//  Request bodies (GoTrue REST API)
// ─────────────────────────────────────────────

@Serializable
data class SignUpRequest(
    val email: String,
    val password: String
)

@Serializable
data class SignInRequest(
    val email: String,
    val password: String
)

@Serializable
data class IdTokenSignInRequest(
    val provider: String = "google",
    @SerialName("id_token") val idToken: String
)

@Serializable
data class RefreshTokenRequest(
    @SerialName("refresh_token")
    val refreshToken: String
)

// ─────────────────────────────────────────────
//  Response bodies (GoTrue REST API)
// ─────────────────────────────────────────────

/**
 * Successful auth response from GoTrue (sign-up, sign-in, token refresh).
 * [expiresIn] is seconds until the access token expires (typically 3600).
 */
@Serializable
data class GoTrueResponse(
    @SerialName("access_token")
    val accessToken: String = "",

    @SerialName("refresh_token")
    val refreshToken: String = "",

    @SerialName("expires_in")
    val expiresIn: Long = 3600L,

    val user: GoTrueUser? = null
)

@Serializable
data class GoTrueUser(
    val id: String = "",
    val email: String? = null
)

/**
 * Error response from GoTrue.
 * Returned as JSON body when the HTTP status is 4xx.
 */
@Serializable
data class GoTrueError(
    val error: String = "",
    @SerialName("error_description")
    val errorDescription: String = "",
    // Some GoTrue errors use "msg" instead of "error_description"
    val msg: String = ""
) {
    /** Human-readable error message (normalises the two possible field names). */
    val message: String get() = errorDescription.ifBlank { msg }.ifBlank { error }
}
