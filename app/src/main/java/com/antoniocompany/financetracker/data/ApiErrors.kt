package com.antoniocompany.financetracker.data

import retrofit2.HttpException

private val CODE = Regex("\"code\"\\s*:\\s*\"([a-z_]+)\"")

/**
 * El codigo de error que manda la API en su ProblemDetails ("invalid_mfa_code",
 * "mfa_already_enabled"...), o null si no hay. Se decide por el codigo, nunca
 * por el texto, igual que en la web y en escritorio.
 *
 * Ojo: el cuerpo de un error solo se puede leer una vez.
 */
fun HttpException.apiErrorCode(): String? {
    val body = runCatching { response()?.errorBody()?.string() }.getOrNull() ?: return null
    return CODE.find(body)?.groupValues?.get(1)
}
