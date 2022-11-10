package com.example.websocketsbutton

import android.util.Log
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

class WebSocketListener : WebSocketListener(){

    override fun onOpen(webSocket: WebSocket, response: Response) {
        webSocket.send("Hello World")
        Log.i("Alex", "connected")
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        output("Received: $text")
    }

    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
        webSocket.close(NORMAL_CLOUSURE_STATUS, null)
        output("Closing: $code / $reason")
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        Log.e(webSocket.toString(), "Error: " + t.message)
    }

    private fun output(text: String) {
        Log.d("Websockets", text!!)
    }

    companion object {
        private const val NORMAL_CLOUSURE_STATUS = 1000
    }

}