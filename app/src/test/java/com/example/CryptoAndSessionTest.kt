package com.example

import com.example.data.model.AutoLockPolicy
import com.example.data.repository.AppSessionManager
import com.example.security.CryptoManager
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class CryptoAndSessionTest {

    @Test
    fun `pin hashing verifies demo PIN 8546 and rejects wrong PIN`() {
        val crypto = CryptoManager()
        val (hash, salt) = crypto.hashSecret("8546")

        assertTrue(crypto.verifySecret("8546", hash, salt))
        assertFalse(crypto.verifySecret("1234", hash, salt))
    }

    @Test
    fun `stream encryption and decryption roundtrip preserves binary payload`() {
        val crypto = CryptoManager()
        val original = ByteArray(150_000) { (it % 251).toByte() }
        val encryptedOut = ByteArrayOutputStream()

        val plainCount = crypto.encryptStream(ByteArrayInputStream(original), encryptedOut)
        assertEquals(original.size.toLong(), plainCount)

        val decryptedOut = ByteArrayOutputStream()
        crypto.decryptStream(ByteArrayInputStream(encryptedOut.toByteArray()), decryptedOut)
        assertArrayEquals(original, decryptedOut.toByteArray())
    }

    @Test
    fun `screen off clears auto-lock session unlocks`() {
        AppSessionManager.clearAllUnlocked()
        AppSessionManager.markAppUnlocked("com.example.test", AutoLockPolicy.MINUTES_5)
        assertTrue(AppSessionManager.isAppTemporarilyUnlocked("com.example.test", AutoLockPolicy.MINUTES_5))

        AppSessionManager.onScreenTurnedOff()
        assertFalse(AppSessionManager.isAppTemporarilyUnlocked("com.example.test", AutoLockPolicy.MINUTES_5))
    }
}
