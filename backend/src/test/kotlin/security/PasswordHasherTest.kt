package com.japp.security

import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals

class PasswordHasherTest {

    private val hasher = PasswordHasher()

    @Test
    fun `hash should generate different hashes for same password`() {
        val password = "testPassword123"
        val hash1 = hasher.hash(password)
        val hash2 = hasher.hash(password)

        assertNotEquals(hash1, hash2, "BCrypt should produce different salts")
    }

    @Test
    fun `verify should return true for correct password`() {
        val password = "correctPassword"
        val hash = hasher.hash(password)

        assertTrue(hasher.verify(password, hash))
    }

    @Test
    fun `verify should return false for incorrect password`() {
        val password = "correctPassword"
        val wrongPassword = "wrongPassword"
        val hash = hasher.hash(password)

        assertFalse(hasher.verify(wrongPassword, hash))
    }

    @Test
    fun `verify should return false for empty password against valid hash`() {
        val hash = hasher.hash("validPassword")

        assertFalse(hasher.verify("", hash))
    }
}