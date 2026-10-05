package app.fibrai.android.core.memory

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/** Seals the memory bytes. The file name is not part of the cipher, so the file can be renamed. */
interface MemoryCipher {
    fun encrypt(plain: ByteArray): ByteArray

    /** Throws when the bytes were not sealed by this key or were altered. */
    fun decrypt(sealed: ByteArray): ByteArray
}

/**
 * AES-256-GCM. Layout: "NM" + version + 12-byte IV + ciphertext with 128-bit tag. The IV comes
 * from the provider on every encrypt (randomized encryption, never reused).
 */
open class AesGcmMemoryCipher(private val key: () -> SecretKey) : MemoryCipher {
    override fun encrypt(plain: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val iv = cipher.iv
        check(iv.size == IV_BYTES) { "unexpected iv" }
        return HEADER + iv + cipher.doFinal(plain)
    }

    override fun decrypt(sealed: ByteArray): ByteArray {
        require(sealed.size > HEADER.size + IV_BYTES && sealed.copyOfRange(0, HEADER.size).contentEquals(HEADER)) { "not a memory file" }
        val iv = sealed.copyOfRange(HEADER.size, HEADER.size + IV_BYTES)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(TAG_BITS, iv))
        return cipher.doFinal(sealed, HEADER.size + IV_BYTES, sealed.size - HEADER.size - IV_BYTES)
    }

    private companion object {
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_BYTES = 12
        const val TAG_BITS = 128
        val HEADER = byteArrayOf('N'.code.toByte(), 'M'.code.toByte(), 1)
    }
}

/** Production key: AES-256 in the Android Keystore, never leaves it. */
@Singleton
class KeystoreMemoryCipher @Inject constructor() : AesGcmMemoryCipher(::keystoreKey)

private const val KEY_ALIAS = "fibrai_memory_v2"

private fun keystoreKey(): SecretKey {
    val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
    (store.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
    val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
    generator.init(
        KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build(),
    )
    return generator.generateKey()
}
