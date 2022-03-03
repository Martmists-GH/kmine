package com.martmists.kmine.ext

import io.ktor.utils.io.*

suspend fun ByteWriteChannel.writeVarInt(i: Int) {
    var i = i
    while (true) {
        if (i and -0x80 == 0) {
            writeByte(i)
            return
        }
        writeByte(i and 0x7F or 0x80)
        i = i ushr 7
    }
}
