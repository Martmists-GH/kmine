package com.martmists.kmine.network

import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

object ConnectionManager {
    private val exec = Executors.newCachedThreadPool()
    private val selector = ActorSelectorManager(exec.asCoroutineDispatcher())
    val factory = aSocket(selector).tcp()

    suspend fun run() {
        val server = factory.bind("0.0.0.0", 25565)
        println("Server started on port 25565")

        coroutineScope {
            launch {
                mainLoop()
            }

            while (true) {
                val socket = server.accept()
                Connection(socket).apply {
                    spawn()
                }
            }
        }
    }

    private suspend fun mainLoop() {
        // TODO
    }
}
