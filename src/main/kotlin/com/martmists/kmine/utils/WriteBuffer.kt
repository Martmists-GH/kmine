package com.martmists.kmine.utils

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*


class WriteBuffer(private val stream: ByteArrayOutputStream) {
    var position = 0
        private set

    private fun writeBE(size: Int, block: (ByteBuffer) -> Unit) {
        val buffer = ByteBuffer.allocate(size).order(ByteOrder.BIG_ENDIAN)
        block(buffer)
        buffer.rewind()
        stream.write(buffer.array())
        position += size
    }

    fun writeBoolean(b: Boolean) {
        stream.write(if (b) 1 else 0)
        position++
    }

    fun writeByte(b: Byte) {
        stream.write(b.toInt())
        position++
    }

    fun writeUByte(b: UByte) {
        writeByte(b.toByte())
    }

    fun writeShort(s: Short) {
        writeBE(2) {
            it.putShort(s)
        }
        position += 2
    }

    fun writeUShort(s: UShort) {
        writeShort(s.toShort())
    }

    fun writeInt(i: Int) {
        writeBE(4) {
            it.putInt(i)
        }
        position += 4
    }

    fun writeUInt(i: UInt) {
        writeInt(i.toInt())
    }

    fun writeLong(l: Long) {
        writeBE(8) {
            it.putLong(l)
        }
        position += 8
    }

    fun writeULong(l: ULong) {
        writeLong(l.toLong())
    }

    fun writeFloat(f: Float) {
        writeBE(4) {
            it.putFloat(f)
        }
        position += 4
    }

    fun writeDouble(d: Double) {
        writeBE(8) {
            it.putDouble(d)
        }
        position += 8
    }

    fun writeVarInt(i: Int) {
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

    fun writeVarLong(l: Long) {
        var l = l
        while (true) {
            if (l and 0x7F.inv() == 0L) {
                writeByte(l.toByte())
                return
            }
            writeByte((l and 0x7F or 0x80).toByte())
            l = l ushr 7
        }
    }

    fun writeString(s: String) {
        val arr = s.toByteArray(Charsets.UTF_8)
        writeVarInt(arr.size)
        writeBytes(arr)
    }

    fun writeBytes(data: ByteArray) {
        stream.writeBytes(data)
        position += data.size
    }

    fun writeUUID(uuid: UUID) {
        writeLong(uuid.mostSignificantBits)
        writeLong(uuid.leastSignificantBits)
    }
}
