package com.martmists.kmine.ext

import io.ktor.utils.io.*
import kotlin.experimental.and

suspend fun ByteReadChannel.readVarInt() : Int {
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
