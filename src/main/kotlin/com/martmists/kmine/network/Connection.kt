package com.martmists.kmine.network

import com.martmists.kmine.network.encryption.CipheredByteReadChannel
import com.martmists.kmine.network.encryption.CipheredByteWriteChannel
import com.martmists.kmine.network.packets.PacketType
import com.martmists.kmine.utils.ReadBuffer
import com.martmists.kmine.utils.WriteBuffer
import io.ktor.network.sockets.*
import io.ktor.util.network.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.channels.ClosedSendChannelException
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.zip.DeflaterOutputStream
import java.util.zip.InflaterInputStream

class Connection(private val socket: Socket) {
    private val dispatcher = Dispatchers.Default
    private val session = ClientNetworkSession(this)
    private var compressed = false

    protected var isClosed = AtomicBoolean(false)
    private val sendQueue = Channel<Pair<Int, ByteArray>>()

    lateinit var readChannel: CipheredByteReadChannel
    lateinit var writeChannel: CipheredByteWriteChannel

    fun CoroutineScope.spawn() {
        launch(dispatcher) {
            readLoop()
        }
        launch(dispatcher) {
            writeLoop()
            onClose()
            sendQueue.close()
        }
    }

    suspend fun send(type: PacketType, block: WriteBuffer.() -> Unit) {
        require(!type.serverbound) { "Cannot send a serverbound packet to the client" }

        println("Sending packet $type")

        val stream = ByteArrayOutputStream()
        val buffer = WriteBuffer(stream)
        buffer.block()
        sendQueue.send(type.value to stream.toByteArray())
    }

    suspend fun send(data: Pair<Int, ByteArray>) {
        sendQueue.send(data)
    }

    fun close() {
        isClosed.set(true)
    }

    private suspend fun onClose() {
        socket.close()
    }

    private suspend fun readPacket(channel: CipheredByteReadChannel) {
        val reader: ReadBuffer

        if (compressed) {
            val lengthPacket = channel.readVarInt()
            val buffer = channel.readBytes(lengthPacket)
            val cReader = ReadBuffer(ByteArrayInputStream(buffer))
            val decompressedSize = cReader.readVarInt()
            val compressedData = cReader.remaining()
            val stream = InflaterInputStream(ByteArrayInputStream(compressedData))
            val decompressed = stream.readNBytes(decompressedSize)
            reader = ReadBuffer(ByteArrayInputStream(decompressed))
        } else {
            val length = channel.readVarInt()
            val buffer = channel.readBytes(length)
            reader = ReadBuffer(ByteArrayInputStream(buffer))
        }

        try {
            session.handlePacket(reader)
        } catch (e: Exception) {
            e.printStackTrace()
        } catch (e: Error) {
            e.printStackTrace()
        }
    }

    private suspend fun writePacket(type: Int, data: ByteArray, out: CipheredByteWriteChannel) {
        if (compressed) {
            var outStream = ByteArrayOutputStream()
            val writer = WriteBuffer(outStream)
            writer.writeVarInt(data.size)
            writer.writeBytes(data)
            val decompressed = outStream.toByteArray()
            outStream = ByteArrayOutputStream()
            val compressor = DeflaterOutputStream(outStream)
            compressor.write(decompressed)
            val compressed = outStream.toByteArray()
            out.writeVarInt(compressed.size)
            out.writeVarInt(decompressed.size)
            out.writeBytes(compressed)
        } else {
            val tmp = ByteArrayOutputStream()
            val writer = WriteBuffer(tmp)
            writer.writeVarInt(type)
            writer.writeBytes(data)
            val res = tmp.toByteArray()
            out.writeVarInt(res.size)
            out.writeBytes(res)
        }
    }

    private suspend fun readLoop() {
        readChannel = CipheredByteReadChannel(socket.openReadChannel())
        while (!isClosed.get()) {
            try {
                readPacket(readChannel)
            } catch (e: TimeoutCancellationException) {
                isClosed.set(true)
            } catch (e: ClosedReceiveChannelException) {
                isClosed.set(true)
            } catch (e: IOException) {
                isClosed.set(true)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private suspend fun writeLoop() {
        writeChannel = CipheredByteWriteChannel(socket.openWriteChannel(true))
        while (!isClosed.get()) {
            try {
                withTimeout(60 * 1000L) {
                    val item = sendQueue.receive()
                    writePacket(item.first, item.second, writeChannel)
                }
            } catch (e: TimeoutCancellationException) {
                // Allow exiting the loop if read times out
            } catch (e: ClosedSendChannelException) {
                isClosed.set(true)
            } catch (e: IOException) {
                isClosed.set(true)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun getAddress(): NetworkAddress {
        return socket.remoteAddress
    }
}
