package com.antoniocompany.financetracker.domain

import com.antoniocompany.financetracker.data.model.CategoryDto
import com.antoniocompany.financetracker.data.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Reglas de los formularios. Cada una evita una peticion a la API o un dato
 * mal guardado, y un fallo aqui no se ve hasta que alguien escribe justo el
 * caso que rompe.
 */
class ValidationTest {

    // --- Recuperar contraseña

    @Test
    fun `a valid reset has no problem`() {
        assertNull(validatePasswordReset("123456", "secreto-largo", "secreto-largo"))
    }

    @Test
    fun `the reset code must be exactly six digits`() {
        listOf("", "12345", "1234567", "12a456", " 123456").forEach { code ->
            assertEquals(
                code,
                PasswordResetProblem.INVALID_CODE_FORMAT,
                validatePasswordReset(code, "secreto-largo", "secreto-largo")
            )
        }
    }

    @Test
    fun `the new password follows the registration rules`() {
        assertEquals(
            PasswordResetProblem.PASSWORD_TOO_SHORT,
            validatePasswordReset("123456", "corta", "corta")
        )
        assertEquals(
            PasswordResetProblem.PASSWORDS_DONT_MATCH,
            validatePasswordReset("123456", "secreto-largo", "secreto-larga")
        )
    }

    @Test
    fun `email check accepts a normal address and rejects a broken one`() {
        assertTrue(isValidEmail("ana@mail.com"))
        assertFalse(isValidEmail("ana@mail"))
    }

    // --- Registro

    @Test
    fun `a complete and valid registration has no problem`() {
        assertNull(validateRegistration("Ana", "ana@mail.com", "secreto-largo", "secreto-largo"))
    }

    @Test
    fun `any empty field is reported first`() {
        assertEquals(
            RegistrationProblem.MISSING_FIELDS,
            validateRegistration("", "ana@mail.com", "secreto-largo", "secreto-largo")
        )
        assertEquals(
            RegistrationProblem.MISSING_FIELDS,
            validateRegistration("Ana", "", "corta", "otra")
        )
    }

    @Test
    fun `an email without domain is rejected`() {
        listOf("ana", "ana@", "ana@mail", "ana @mail.com", "@mail.com").forEach { email ->
            assertEquals(
                "\"$email\"",
                RegistrationProblem.INVALID_EMAIL,
                validateRegistration("Ana", email, "secreto-largo", "secreto-largo")
            )
        }
    }

    @Test
    fun `the password needs the API minimum of twelve characters`() {
        assertEquals(
            RegistrationProblem.PASSWORD_TOO_SHORT,
            validateRegistration("Ana", "ana@mail.com", "12345678901", "12345678901")
        )
        assertNull(validateRegistration("Ana", "ana@mail.com", "123456789012", "123456789012"))
    }

    @Test
    fun `the two passwords must match`() {
        assertEquals(
            RegistrationProblem.PASSWORDS_DONT_MATCH,
            validateRegistration("Ana", "ana@mail.com", "secreto-largo", "Secreto-largo")
        )
    }

    // --- Importe

    @Test
    fun `amounts are read with a comma or a dot`() {
        assertEquals(12.5, parseAmount("12,5")!!, 0.0)
        assertEquals(12.5, parseAmount("12.5")!!, 0.0)
        assertEquals(7.0, parseAmount(" 7 ")!!, 0.0)
    }

    @Test
    fun `text that is not a number gives null`() {
        assertNull(parseAmount(null))
        assertNull(parseAmount(""))
        assertNull(parseAmount("doce"))
        assertNull(parseAmount("1.234,56")) // miles no admitidos: mejor null que 1.234
    }

    @Test
    fun `the amount to edit has no trailing zeros and the language's decimal mark`() {
        assertEquals("12,5", formatAmountForInput(12.50, decimalComma = true))
        assertEquals("12.5", formatAmountForInput(12.50, decimalComma = false))
        assertEquals("750", formatAmountForInput(750.0, decimalComma = true))
        assertEquals("0,1", formatAmountForInput(0.1, decimalComma = true))
    }

    @Test
    fun `an amount survives the round trip from edit field back to number`() {
        listOf(0.1, 12.5, 132.4, 1900.0, 0.01).forEach { amount ->
            assertEquals(amount, parseAmount(formatAmountForInput(amount, decimalComma = true))!!, 0.0)
        }
    }

    // --- Categorias

    private val categories = listOf(
        CategoryDto(1, "Supermercado", TransactionType.EXPENSE),
        CategoryDto(2, "Nomina", TransactionType.INCOME)
    )

    @Test
    fun `a category name is a duplicate regardless of case and spaces`() {
        assertTrue(isDuplicateCategoryName(categories, "supermercado", TransactionType.EXPENSE))
        assertTrue(isDuplicateCategoryName(categories, "  SUPERMERCADO ", TransactionType.EXPENSE))
    }

    @Test
    fun `the same name is allowed for the other type`() {
        assertFalse(isDuplicateCategoryName(categories, "Supermercado", TransactionType.INCOME))
        assertFalse(isDuplicateCategoryName(categories, "Ocio", TransactionType.EXPENSE))
    }
}
