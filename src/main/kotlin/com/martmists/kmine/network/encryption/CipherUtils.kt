package com.martmists.kmine.network.encryption

import java.security.Key
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object CipherUtils {
    fun decryptSecret(key: Key, secret: ByteArray): SecretKey {
        val data = decrypt(key, secret).also {
            println("Private key: ${key.encoded.contentToString()}\nEncrypted Secret Key: ${secret.contentToString()}\nDecrypted Secret Key: ${it.contentToString()}")
        }
        return SecretKeySpec(data, "AES")
    }

    fun encrypt(key: Key, data: ByteArray): ByteArray {
        return crypt(Cipher.ENCRYPT_MODE, key, data)
    }

    fun decrypt(key: Key, data: ByteArray): ByteArray {
        return crypt(Cipher.DECRYPT_MODE, key, data)
    }

    private fun crypt(mode: Int, key: Key, data: ByteArray): ByteArray {
        return crypt(mode, key.algorithm, key).doFinal(data)
    }

    private fun crypt(mode: Int, algorithm: String, key: Key): Cipher {
        val cipher = Cipher.getInstance(algorithm)
        cipher.init(mode, key)
        return cipher
    }

    fun cipherFromKey(mode: Int, key: Key): Cipher {
        val cipher = Cipher.getInstance("AES/CFB8/NoPadding")
        cipher.init(mode, key, IvParameterSpec(key.encoded))
        return cipher
    }

    fun hash(vararg args: ByteArray): ByteArray {
        val sha1 = MessageDigest.getInstance("SHA-1")
        for (arg in args) {
            sha1.update(arg)
        }
        return sha1.digest()
    }

    fun generateServerId(base: String, key: Key, secret: Key) : ByteArray {
        return hash(base.toByteArray(Charsets.ISO_8859_1), secret.encoded, key.encoded)
    }
}
