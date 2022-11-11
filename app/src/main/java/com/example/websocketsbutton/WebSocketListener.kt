package com.example.websocketsbutton

import android.provider.Settings
import android.util.Log
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject

class WebSocketListener (
    APIKey : String
): WebSocketListener(){

    val apiKey = APIKey

    override fun onOpen(webSocket: WebSocket, response: Response) {
        webSocket.send("key")
        Log.i("Alex", "connected")
    }

    override fun onMessage(webSocket: WebSocket, text: String) {

        // dict input for web socket handshake
        val jsonIn = JSONObject(text)
        // Parameters for authentication
        val platform = android.os.Build.MODEL.lowercase()
        val salt = ""
        val resetToken = ""
        val deviceId = Settings.Secure.ANDROID_ID
        val key = apiKey
        val bundleId = "com.samcolak.holler"

        val logInKey = "$platform:$deviceId:$salt:$apiKey"
        //val token =

        // Returns from web socket handshake
        val info = JSONObject()
        info.put("id_bundle", bundleId)
        info.put("version", 1)
        info.put("manu", "android") // manufacturer
        info.put("platform", platform)
        info.put("format", "phone")
        info.put("mode", "development")

        if (jsonIn.get("success") == 1) {
            if (jsonIn.has("target")) {
                val target : JSONObject = jsonIn.getJSONObject("target")
                val event = target.get("event")

                if (event == "init") {
                    val data = JSONObject()
                    val actionOutput = JSONObject()
                    actionOutput.put("type", "auth.bindSession")
                    actionOutput.put("transid", "Alex1234")
                    actionOutput.put("data", data)

                    val jsonOut = JSONObject()
                    jsonOut.put("actions", JSONArray().apply {
                        put(actionOutput)
                    })

                    webSocket.send(jsonOut.toString())
                }
            }
        }
        output("Received: $text")
    }

    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
        webSocket.close(NORMAL_CLOSURE_STATUS, null)
        output("Closing: $code / $reason")
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        Log.e(webSocket.toString(), "Error: " + t.message)
    }

    private fun output(text: String) {
        Log.d("Websockets", text)
    }

    companion object {
        private const val NORMAL_CLOSURE_STATUS = 1000
    }

}