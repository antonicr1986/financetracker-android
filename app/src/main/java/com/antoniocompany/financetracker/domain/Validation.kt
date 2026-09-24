package com.antoniocompany.financetracker.domain

import com.antoniocompany.financetracker.data.model.CategoryDto
import com.antoniocompany.financetracker.data.model.TransactionType
import java.math.BigDecimal

/*
 * Reglas de los formularios, en Kotlin puro.
 *
 * Viven aqui y no en las Activities por lo mismo que Derive.kt: sin Android
 * de por medio se prueban en la JVM en milisegundos. Las pantallas solo
 * traducen el resultado a un texto.
 */

/** Lo primero que falla en el formulario de registro, en el orden de la web. */
enum class RegistrationProblem {
    MISSING_FIELDS,
    INVALID_EMAIL,
    PASSWORD_TOO_SHORT,
    PASSWORDS_DONT_MATCH
}

/** Minimo de la API (RegisterUserDto). */
const val MIN_PASSWORD_LENGTH = 6

/**
 * Forma basica de un correo: algo@algo.algo, sin espacios. No pretende ser
 * exhaustiva; la API valida el resto. Es una expresion propia y no
 * android.util.Patterns para que esto no dependa de Android.
 */
private val EMAIL = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

/** null si el formulario es valido. Espera los textos ya recortados. */
fun validateRegistration(
    name: String,
    email: String,
    password: String,
    confirmation: String
): RegistrationProblem? = when {
    name.isEmpty() || email.isEmpty() || password.isEmpty() -> RegistrationProblem.MISSING_FIELDS
    !EMAIL.matches(email) -> RegistrationProblem.INVALID_EMAIL
    password.length < MIN_PASSWORD_LENGTH -> RegistrationProblem.PASSWORD_TOO_SHORT
    password != confirmation -> RegistrationProblem.PASSWORDS_DONT_MATCH
    else -> null
}

/**
 * Lee el importe escrito. Acepta coma o punto como decimal, porque la coma es
 * lo que teclea cualquiera en espanol y toDouble() solo entiende el punto.
 * null si no es un numero; no comprueba que sea positivo.
 */
fun parseAmount(text: String?): Double? =
    text?.trim()?.replace(",", ".")?.toDoubleOrNull()

/**
 * Importe para rellenar el campo al editar: sin moneda, sin miles y sin ceros
 * de sobra (12.5, no 12.50). Con coma si se esta en espanol.
 */
fun formatAmountForInput(amount: Double, decimalComma: Boolean): String {
    val plain = BigDecimal(amount.toString()).stripTrailingZeros().toPlainString()
    return if (decimalComma) plain.replace(".", ",") else plain
}

/**
 * Si ya hay una categoria del mismo tipo con ese nombre, sin distinguir
 * mayusculas ni espacios de los extremos. Del otro tipo si se permite: puede
 * haber "Regalos" de gasto y de ingreso.
 */
fun isDuplicateCategoryName(
    categories: List<CategoryDto>,
    name: String,
    type: TransactionType
): Boolean {
    val wanted = name.trim()
    return categories.any { it.type == type && it.name.trim().equals(wanted, ignoreCase = true) }
}
