package com.martmists.kmine.network.encryption

import io.ktor.utils.io.*
import java.security.Key
import javax.crypto.Cipher

class CipheredByteWriteChannel(private val parent: ByteWriteChannel) {
    private var key: Key? = null
        set(value) {
            field = value
            cipher = Cipher.getInstance("AES")
            cipher!!.init(Cipher.ENCRYPT_MODE, key)
        }
    var cipher: Cipher? = null

    suspend fun writeBytes(b: ByteArray) {
        cipher?.let {
            val res = it.update(b)
            parent.writeFully(res)
        } ?: let {
            parent.writeFully(b)
        }
    }

    suspend fun writeByte(b: Byte) {
        writeBytes(byteArrayOf(b))
    }

    suspend fun writeVarInt(i: Int) {
        var i = i
        while (true) {
            if (i and -0x80 == 0) {
                writeByte(i.toByte())
                return
            }
            writeByte((i and 0x7F or 0x80).toByte())
            i = i ushr 7
        }
    }
}
