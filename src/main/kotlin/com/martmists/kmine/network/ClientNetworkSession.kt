package com.martmists.kmine.network

import com.martmists.kmine.network.encryption.CipherUtils
import com.martmists.kmine.network.packets.PacketType
import com.martmists.kmine.network.packets.PacketType.*
import com.martmists.kmine.network.packets.ProtocolState
import com.martmists.kmine.network.packets.ProtocolState.*
import com.martmists.kmine.network.payloads.*
import com.martmists.kmine.utils.ReadBuffer
import io.ktor.client.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.request.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.math.BigInteger
import java.security.KeyPairGenerator
import java.util.*
import javax.crypto.Cipher

class ClientNetworkSession(private val connection: Connection) {
    companion object {
        private val client = HttpClient {
            install(JsonFeature) {
                serializer = KotlinxSerializer()
            }
        }
        private val serverId = ""
        private val keypair = KeyPairGenerator.getInstance("RSA").apply {
            initialize(1024)
        }.generateKeyPair()
    }

    private var state = ProtocolState.HANDSHAKE
    private val nonce = byteArrayOf(3, 36, -60, -98)  // Random.nextBytes(4)

    private var username: String = "Player"

    suspend fun handlePacket(data: ReadBuffer) {
        val typeId = data.readVarInt()
        val type = PacketType.get(state, true, typeId) ?: TODO("Unknown packet type $typeId while in state $state")

        println("Handling packet of type $type")

        when (state) {
            ProtocolState.HANDSHAKE -> {
                when (type) {
                    PacketType.HANDSHAKE -> {
                        val protocolVersion = data.readVarInt()
                        val serverAddress = data.readString()
                        val serverPort = data.readUShort()
                        val nextState = data.readVarInt()

                        when (nextState) {
                            1 -> {
                                state = STATUS
                            }
                            2 -> {
                                state = LOGIN
                            }
                            else -> {
                                println("Unknown next state $nextState")
                                connection.close()
                            }
                        }
                    }
                    else -> {
                        println("Unhandled packet type $type while in state $state")
                        connection.close()
                    }
                }
            }

            STATUS -> {
                when (type) {
                    STATUS_REQUEST -> {
                        val data = Json.encodeToString(
                            StatusResponse(
                                VersionInfo(
                                    "1.18.1",
                                    757,
                                ),
                                PlayersInfo(
                                    max = 100,
                                    online = 0,
                                    sample = listOf()
                                ),
                                DescriptionInfo(
                                    "Dummy info here"
                                )
                            )
                        )
                        connection.send(STATUS_RESPONSE) {
                            writeString(data)
                        }
                    }
                    STATUS_PING -> {
                        val payload = data.readLong()
                        connection.send(STATUS_PONG) {
                            writeLong(payload)
                        }
                    }
                    else -> {
                        println("Unhandled packet type $type while in state $state")
                        connection.close()
                    }
                }
            }

            LOGIN -> {
                when (type) {
                    LOGIN_START -> {
                        username = data.readString()

                        println("Authenticating as $username")

                        connection.send(LOGIN_ENCRYPT_REQUEST) {
                            writeString(serverId)
                            writeVarInt(keypair.public.encoded.size)
                            writeBytes(keypair.public.encoded)
                            writeVarInt(nonce.size)
                            writeBytes(nonce)
                        }
                    }
                    LOGIN_ENCRYPT_RESPONSE -> {
                        val secretEnc = data.readBytes(data.readVarInt())
                        val tokenEnc = data.readBytes(data.readVarInt())

                        val token = CipherUtils.decrypt(keypair.private, tokenEnc)

                        if (!token.contentEquals(nonce)) {
                            println("Invalid token")
                            connection.send(LOGIN_DISCONNECT) {
                                writeString("Invalid token")
                            }
                            connection.close()
                            return
                        }

                        val secret = CipherUtils.decryptSecret(keypair.private, secretEnc)
                        val encCipher = CipherUtils.cipherFromKey(Cipher.ENCRYPT_MODE, secret)
                        val decCipher = CipherUtils.cipherFromKey(Cipher.DECRYPT_MODE, secret)
                        connection.readChannel.cipher = decCipher
                        connection.writeChannel.cipher = encCipher

                        val hash = BigInteger(CipherUtils.generateServerId(serverId, keypair.public, secret)).toString(16)

                        val url = "https://sessionserver.mojang.com/session/minecraft/hasJoined?username=$username&serverId=$hash"
                        println("Requesting session from $url")
                        val res = client.get<UserPayload>(url) {

                        }
                        println(res)

//                        connection.send(LOGIN_SET_COMPRESSION) {
//                            writeVarInt(1)
//                        }

                        state = PLAY
                        connection.send(LOGIN_SUCCESS) {
                            writeUUID(UUID.fromString(res.id))
                            writeString(username)
                        }
                    }
                    LOGIN_PLUGIN_RESPONSE -> {

                    }
                    else -> {
                        println("Unhandled packet type $type while in state $state")
                        connection.close()
                    }
                }
            }

            PLAY -> {
                TODO("Play")
            }
        }
    }
}
