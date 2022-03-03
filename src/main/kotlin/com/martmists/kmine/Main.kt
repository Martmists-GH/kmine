package com.martmists.kmine

import com.martmists.kmine.network.ConnectionManager
import kotlinx.coroutines.runBlocking

object Main {
    @JvmStatic
    fun main(args: Array<String>) = runBlocking {
        ConnectionManager.run()
    }
}
