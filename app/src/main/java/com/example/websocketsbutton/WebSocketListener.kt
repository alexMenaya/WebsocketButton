package com.example.websocketsbutton

import android.provider.Settings
import android.util.Log
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import java.nio.charset.StandardCharsets.UTF_8
import java.security.MessageDigest
import java.util.*
import java.util.concurrent.TimeUnit

class WebSocketListener (
    private val deviceId : String
): WebSocketListener(){

    override fun onOpen(webSocket: WebSocket, response: Response) {
        webSocket.send("key")
        Log.i("Alex", "connected")
        Log.i("Alex", deviceId)
    }

    fun md5(str: String): ByteArray = MessageDigest.getInstance("MD5")
        .digest(str.toByteArray(UTF_8))
    fun ByteArray.toHex() = joinToString(separator = "") { byte -> "%02x".format(byte) }

    override fun onMessage(webSocket: WebSocket, text: String) {

        // dict input for web socket handshake
        val jsonIn = JSONObject(text)
        // Parameters for authentication

        // Returns from web socket handshake

        if (jsonIn.get("success") == 1) {

            if (jsonIn.has("transid")) {

                val transid = jsonIn.get("transid")

                if (transid == "SYSTEM") {

                    val dataobject = jsonIn.getJSONObject("data")

                    val scope = dataobject.get("scope")

                    if (scope == "update") { // this is data coming in !!!!

                        val mappings = mapOf<String, String>(
                            "hollers" to "Holler",
                            "friends" to "Friend",
                            "people" to "Person",
                            "messages" to "Message",
                            "participants" to "Participant",
                            "groups" to "Group",
                            "groupmembers" to "GroupMember",
                            "pics" to "MediaItem",
                            "advertisers" to "Advertiser",
                            "adlocations" to "AdLocation",
                            "places" to "Place",
                            "blocks" to "Block"
                        )

                        var domains = mappings.keys

                        for (domain in domains) {
                            if (dataobject.has(domain)) {
                                var domaintoimport = dataobject.get(domain)
                                output("$domain = $domaintoimport")
                            }
                        }

                    } else if (scope == "message") { // this is info !!

                    }

                } else if (transid == "bindalex") {

                    var dataobject = jsonIn.getJSONObject("data")

                    var sessionId = dataobject.get("sessionid")
                    var authToken = dataobject.get("token")

                    val platform = android.os.Build.MODEL.lowercase()
                    var salt: String? = "ff1b4ab569fd790ca111188927da7515";
                    val resetToken = "5d607b3ea97eb67f4d025298aa4a23cd"

                    if (salt == null) salt = resetToken;

                    val apiKey = "d8a39f7a48cfeac9321b85aad28cf74f"
                    val deviceId = "999999888abcd"
                    val key = apiKey
                    val bundleId = "com.samcolak.holler"

                    val logInKey = "$platform:$deviceId:$salt:$apiKey"
                    var token:ByteArray = md5(logInKey).toHex().toString().toByteArray()

                    var hmacAlgorithm = HmacAlgorithm.SHA1
                    var config = TimeBasedOneTimePasswordConfig(30, TimeUnit.SECONDS,6, hmacAlgorithm)
                    val date = Date()
                    var topt = TimeBasedOneTimePasswordGenerator(token, config).generate(Date())

                    val info = JSONObject()
                    info.put("id_bundle", bundleId)
                    info.put("version", 1)
                    info.put("manu", "google") // manufacturer
                    info.put("platform", platform)
                    info.put("format", "phone")
                    info.put("mode", "development")

                    val auth = JSONObject()
                    auth.put("token", topt)
                    auth.put("deviceid", deviceId)

                    val data = JSONObject()
                    data.put("info", info)
                    data.put("auth", auth)

                    val actionOutput = JSONObject()
                    actionOutput.put("type", "auth.authDevice")
                    actionOutput.put("transid", "loginalex")
                    actionOutput.put("data", data)

                    val jsonOut = JSONObject()
                    jsonOut.put("actions", JSONArray().apply {
                        put(actionOutput)
                    })

                    webSocket.send(jsonOut.toString())

                }

            } else {

                if (jsonIn.has("target")) {
                    val target: JSONObject = jsonIn.getJSONObject("target")
                    val event = target.get("event")

                    if (event == "init") {
                        val data = JSONObject()
                        val actionOutput = JSONObject()
                        actionOutput.put("type", "auth.bindSession")
                        actionOutput.put("transid", "bindalex")
                        actionOutput.put("data", data)

                        val jsonOut = JSONObject()
                        jsonOut.put("actions", JSONArray().apply {
                            put(actionOutput)
                        })

                        webSocket.send(jsonOut.toString())
                    }
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