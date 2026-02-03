package com.example.finfy.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LoginValidatorTest {
    @Test
    fun `valid email and password passes`() {
        val result = LoginValidator.validate("user@finfy.com", "secret")
        assertTrue(result.isValid)
        assertEquals(null, result.emailError)
        assertEquals(null, result.passwordError)
    }

    @Test
    fun `invalid email fails`() {
        val result = LoginValidator.validate("invalid-email", "secret")
        assertFalse(result.isValid)
        assertEquals("Digite um e-mail válido.", result.emailError)
        assertEquals(null, result.passwordError)
    }

    @Test
    fun `empty password fails`() {
        val result = LoginValidator.validate("user@finfy.com", "   ")
        assertFalse(result.isValid)
        assertEquals(null, result.emailError)
        assertEquals("Informe e-mail e senha para continuar.", result.passwordError)
    }
}
