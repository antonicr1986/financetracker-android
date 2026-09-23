package com.antoniocompany.financetracker.data.model

/** Cuerpo de POST /api/Users/login. */
data class LoginRequest(
    val email: String,
    val password: String
)

/**
 * Cuerpo de POST /api/Users/register. Mismos limites que RegisterUserDto en
 * la API: nombre hasta 100, correo hasta 150 y contrasena de al menos 6.
 */
data class RegisterRequest(
    val name: String,
    val email: String,
    val password: String
)

/**
 * Respuesta de POST /api/Users/login.
 *
 * El registro, en cambio, devuelve el usuario y no un token: despues de
 * registrar hay que encadenar el login, como hace la web.
 */
data class LoginResponse(
    val token: String,
    val expiration: String,
    val user: UserDto?
)

data class UserDto(
    val id: Int,
    val name: String?,
    val email: String
)
