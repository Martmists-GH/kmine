package com.martmists.kmine.utils

import java.io.ByteArrayInputStream
import java.nio.ByteBuffer
import java.util.*
import kotlin.experimental.and

class ReadBuffer(private val stream: ByteArrayInputStream) {
    private fun readBE(size: Int): ByteBuffer {
        val b = ByteArray(size)
        stream.read(b)
        return ByteBuffer.wrap(b)
    }

    // Everything is big endian
    fun readBoolean() : Boolean {
        return stream.read() == 1
    }

    fun readByte() : Byte {
        return stream.read().toByte()
    }

    fun readUByte() : UByte {
        return readByte().toUByte()
    }

    fun readShort() : Short {
        return readBE(2).short
    }

    fun readUShort() : UShort {
        return readShort().toUShort()
    }

    fun readInt() : Int {
        return readBE(4).int
    }

    fun readUInt() : UInt {
        return readInt().toUInt()
    }

    fun readLong() : Long {
        return readBE(8).long
    }

    fun readULong() : ULong {
        return readLong().toULong()
    }

    fun readFloat() : Float {
        return readBE(4).float
    }

    fun readDouble() : Double {
        return readBE(8).double
    }

    fun readVarInt() : Int {
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

    fun readVarLong() : Long {
        var value: Long = 0
        var length = 0
        var currentByte: Byte

        while (true) {
            currentByte = readByte()
            value = value or ((currentByte and 0x7F).toLong() shl length * 7)
            length += 1
            if (length > 10) {
                throw RuntimeException("VarLong is too big")
            }
            if ((currentByte.toInt() and 0x80) != 0x80) {
                break
            }
        }
        return value
    }

    fun readString() : String {
        val length = readVarInt()
        return ByteArray(length) { readByte() }.toString(Charsets.UTF_8)
    }

    fun remaining(): ByteArray {
        return stream.readAllBytes()
    }

    fun readBytes(size: Int): ByteArray {
        return ByteArray(size) { readByte() }
    }

    fun readUUID(): UUID {
        val m = readLong()
        val l = readLong()
        return UUID(l, m)
    }
}
