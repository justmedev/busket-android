package dev.justme.busket.feathers.responses

data class AuthenticationSuccessResponse(
    val accessToken: String,
    val authentication: Authentication,
    val user: User,
)

data class Authentication(
    val strategy: String,
    val accessToken: String,
    val payload: AuthenticationPayload
)

data class AuthenticationPayload(
    val iat: Int,
    val exp: Int,
    val aud: String,
    val iss: String,
    val sub: String,
    val jti: String,
)

data class User(
    val id: Int,
    val uuid: String,
    val email: String,
    val fullName: String,
    val avatarURI: String?,
    val preferredLanguage: String,
    val prefersDarkMode: Boolean,
    val prefersMiniDrawer: Boolean,
    val googleId: String?,
    val githubId: String?,
    val createdAt: String,
    val updatedAt: String,
)
