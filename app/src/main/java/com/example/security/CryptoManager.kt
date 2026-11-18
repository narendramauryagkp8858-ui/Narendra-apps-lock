package com.example.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec

class CryptoManager {

    companion object {
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val KEY_ALIAS = "NarendraAppLockMasterKey_v1"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH_BITS = 128
        private const val IV_SIZE_BYTES = 12
        private const val CHUNK_SIZE_BYTES = 64 * 1024
        private const val STREAM_MAGIC = 0x4E414C31 // "NAL1"
        private const val PBKDF2_ITERATIONS = 12_000
        private const val SALT_BYTES = 16
        private const val KEY_LENGTH_BITS = 256
    }

    private val secureRandom = SecureRandom()
    @Volatile
    private var testFallbackKey: SecretKey? = null

    private fun getOrCreateMasterKey(): SecretKey {
        return try {
            val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
            val existing = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
            if (existing != null) return existing

            val keyGen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
            val spec = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setRandomizedEncryptionRequired(true)
                .build()
            keyGen.init(spec)
            keyGen.generateKey()
        } catch (_: Exception) {
            testFallbackKey ?: synchronized(this) {
                testFallbackKey ?: KeyGenerator.getInstance("AES").apply { init(256) }.generateKey().also {
                    testFallbackKey = it
                }
            }
        }
    }

    fun encryptBytes(plainBytes: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateMasterKey())
        val iv = cipher.iv
        val cipherBytes = cipher.doFinal(plainBytes)
        return ByteBuffer.allocate(4 + iv.size + cipherBytes.size)
            .putInt(iv.size)
            .put(iv)
            .put(cipherBytes)
            .array()
    }

    fun decryptBytes(encryptedPayload: ByteArray): ByteArray {
        val buffer = ByteBuffer.wrap(encryptedPayload)
        val ivLen = buffer.int
        require(ivLen in 12..16) { "Invalid IV length: $ivLen" }
        val iv = ByteArray(ivLen)
        buffer.get(iv)
        val cipherBytes = ByteArray(buffer.remaining())
        buffer.get(cipherBytes)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateMasterKey(), GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
        return cipher.doFinal(cipherBytes)
    }

    fun encryptString(plainText: String): String {
        val encrypted = encryptBytes(plainText.toByteArray(Charsets.UTF_8))
        return encrypted.toHexString()
    }

    fun decryptString(encodedCipherText: String): String {
        val raw = encodedCipherText.hexToByteArray()
        return String(decryptBytes(raw), Charsets.UTF_8)
    }

    /**
     * Encrypts arbitrarily large media streams in authenticated 64KB AES-256-GCM chunks
     * so large videos never cause OutOfMemoryError.
     */
    fun encryptStream(input: InputStream, output: OutputStream): Long {
        val key = getOrCreateMasterKey()
        val magicBytes = ByteBuffer.allocate(4).putInt(STREAM_MAGIC).array()
        output.write(magicBytes)

        val buffer = ByteArray(CHUNK_SIZE_BYTES)
        var totalPlainBytes = 0L
        while (true) {
            val read = input.read(buffer)
            if (read <= 0) break
            totalPlainBytes += read
            val chunk = if (read == buffer.size) buffer else buffer.copyOf(read)

            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, key)
            val iv = cipher.iv
            val encryptedChunk = cipher.doFinal(chunk)

            val header = ByteBuffer.allocate(8)
                .putInt(iv.size)
                .putInt(encryptedChunk.size)
                .array()
            output.write(header)
            output.write(iv)
            output.write(encryptedChunk)
        }
        output.flush()
        return totalPlainBytes
    }

    fun decryptStream(input: InputStream, output: OutputStream) {
        val key = getOrCreateMasterKey()
        val magicBuf = ByteArray(4)
        if (readFully(input, magicBuf) < 4) error("Corrupted vault stream")
        val magic = ByteBuffer.wrap(magicBuf).int
        require(magic == STREAM_MAGIC) { "Unsupported vault file header" }

        val headerBuf = ByteArray(8)
        while (true) {
            val headerRead = readFully(input, headerBuf)
            if (headerRead == 0) break
            require(headerRead == 8) { "Truncated chunk header" }

            val header = ByteBuffer.wrap(headerBuf)
            val ivLen = header.int
            val cipherLen = header.int
            require(ivLen in 12..16 && cipherLen > 0) { "Invalid chunk metadata" }

            val iv = ByteArray(ivLen)
            val cipherChunk = ByteArray(cipherLen)
            require(readFully(input, iv) == ivLen) { "Truncated IV" }
            require(readFully(input, cipherChunk) == cipherLen) { "Truncated cipher block" }

            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
            val plain = cipher.doFinal(cipherChunk)
            output.write(plain)
        }
        output.flush()
    }

    fun decryptStreamToBytes(input: InputStream): ByteArray {
        val out = ByteArrayOutputStream()
        decryptStream(input, out)
        return out.toByteArray()
    }

    private fun readFully(input: InputStream, dest: ByteArray): Int {
        var offset = 0
        while (offset < dest.size) {
            val read = input.read(dest, offset, dest.size - offset)
            if (read == -1) break
            offset += read
        }
        return offset
    }

    fun hashSecret(secret: String): Pair<String, String> {
        val salt = ByteArray(SALT_BYTES)
        secureRandom.nextBytes(salt)
        val hash = pbkdf2(secret, salt)
        return hash.toHexString() to salt.toHexString()
    }

    fun verifySecret(candidate: String, storedHashHex: String, storedSaltHex: String): Boolean {
        if (storedHashHex.isBlank() || storedSaltHex.isBlank()) return false
        return try {
            val salt = storedSaltHex.hexToByteArray()
            val expectedHash = storedHashHex.hexToByteArray()
            val actualHash = pbkdf2(candidate, salt)
            MessageDigest.isEqual(expectedHash, actualHash)
        } catch (_: Exception) {
            false
        }
    }

    fun generateRecoveryCode(): String {
        val number = secureRandom.nextInt(900_000) + 100_000
        return number.toString()
    }

    private fun pbkdf2(secret: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(secret.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_LENGTH_BITS)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return factory.generateSecret(spec).encoded
    }

    private fun ByteArray.toHexString(): String = joinToString("") { "%02x".format(it) }

    private fun String.hexToByteArray(): ByteArray {
        require(length % 2 == 0) { "Invalid hex length" }
        return ByteArray(length / 2) { i ->
            substring(i * 2, i * 2 + 2).toInt(16).toByte()
        }
    }
}
