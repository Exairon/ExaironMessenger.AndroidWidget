package com.exairon.widget.socket

import com.exairon.widget.model.Service
import io.socket.client.IO
import io.socket.client.Socket
import java.net.URISyntaxException

object SocketHandler {
    lateinit var mSocket: Socket

    @Synchronized
    fun setSocket(): Socket {
        try {
            if (this::mSocket.isInitialized) {
                return mSocket
            }
            val opts = IO.Options()
            opts.path = "/socket"
            val service = Service.getInstance()
            mSocket = IO.socket(service.url, opts)
        } catch (e: URISyntaxException) {

        }
        return mSocket
    }

    @Synchronized
    fun getSocket(): Socket {
        return mSocket
    }

    @Synchronized
    fun establishConnection() {
        mSocket.connect()
    }

    @Synchronized
    fun closeConnection() {
        mSocket.disconnect()
    }
}