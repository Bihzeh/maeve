package gg.maeve.launcher.auth

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermissions
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.io.path.exists

/**
 * Secure, client-side storage for Microsoft refresh tokens. Tokens NEVER leave the
 * user's machine and are NEVER sent to the Maeve backend (ADR-0006).
 */
interface TokenStore {
    fun saveRefreshToken(account: String, refreshToken: String)
    fun loadRefreshToken(account: String): String?
    fun clear(account: String)
}

/**
 * File-backed store using AES-256-GCM with a locally-generated key.
 *
 * NOTE: a key file sitting beside the ciphertext is obfuscation, not strong
 * encryption-at-rest (an attacker with disk access gets both). The security property
 * that matters here — tokens are local-only, never sent to a server — holds. Hardening
 * to the OS credential store (Windows DPAPI / macOS Keychain / libsecret) is a
 * follow-up; this interface lets that swap in without touching callers.
 */
class FileTokenStore(private val dir: Path) : TokenStore {
    private val keyFile: Path get() = dir.resolve("auth.key")
    private fun tokenFile(account: String): Path = dir.resolve("token-${sanitize(account)}.enc")

    override fun saveRefreshToken(account: String, refreshToken: String) {
        Files.createDirectories(dir)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val iv = ByteArray(12).also { SecureRandom().nextBytes(it) }
        cipher.init(Cipher.ENCRYPT_MODE, key(), GCMParameterSpec(128, iv))
        val ct = cipher.doFinal(refreshToken.toByteArray(Charsets.UTF_8))
        val out = iv + ct
        Files.write(tokenFile(account), Base64.getEncoder().encode(out))
        restrict(tokenFile(account))
    }

    override fun loadRefreshToken(account: String): String? {
        val file = tokenFile(account)
        if (!file.exists()) return null
        return runCatching {
            val raw = Base64.getDecoder().decode(Files.readAllBytes(file))
            val iv = raw.copyOfRange(0, 12)
            val ct = raw.copyOfRange(12, raw.size)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, iv))
            String(cipher.doFinal(ct), Charsets.UTF_8)
        }.getOrNull()
    }

    override fun clear(account: String) {
        Files.deleteIfExists(tokenFile(account))
    }

    private fun key(): SecretKeySpec {
        Files.createDirectories(dir)
        val bytes = if (keyFile.exists()) {
            Base64.getDecoder().decode(Files.readAllBytes(keyFile))
        } else {
            val k = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey().encoded
            Files.write(keyFile, Base64.getEncoder().encode(k)); restrict(keyFile)
            k
        }
        return SecretKeySpec(bytes, "AES")
    }

    private fun restrict(path: Path) {
        runCatching { Files.setPosixFilePermissions(path, PosixFilePermissions.fromString("rw-------")) }
    }

    private fun sanitize(account: String) = account.replace(Regex("[^A-Za-z0-9_.-]"), "_")
}
