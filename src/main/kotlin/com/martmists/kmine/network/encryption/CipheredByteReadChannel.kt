package com.martmists.kmine.network.encryption

import io.ktor.utils.io.*
import java.security.Key
import javax.crypto.Cipher
import kotlin.experimental.and

class CipheredByteReadChannel(private val parent: ByteReadChannel) {
    private var key: Key? = null
        set(value) {
            field = value
            cipher = Cipher.getInstance("AES")
            cipher!!.init(Cipher.DECRYPT_MODE, key)
        }
    var cipher: Cipher? = null

    suspend fun readBytes(count: Int): ByteArray {
        val bytes = ByteArray(count)
        val read = parent.readAvailable(bytes)
        return cipher?.update(bytes) ?: bytes
    }

    suspend fun readByte(): Byte {
        return cipher?.let {
            val b = parent.readByte()
            it.update(byteArrayOf(b))[0]
        } ?: parent.readByte()
    }

    suspend fun readVarInt() : Int {
        var value = 0
        var length = 0
        var currentByte: Byte

        while (true) {
            currentByte = readByte()
            value = value or ((currentByte and 0x7F).toInt() shl length * 7)
            length += 1
            if (length > 5) {
                throw RuntimeException("VarInt is too big")
            }
            if ((currentByte.toInt() and 0x80) != 0x80) {
                break
            }
        }
        return value
    }
}
