package com.example.websocketsbutton

import android.annotation.SuppressLint
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import com.example.websocketsbutton.databinding.ActivityMainBinding
import okhttp3.*
import okhttp3.internal.ws.RealWebSocket
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    @SuppressLint("HardwareIds")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val client = OkHttpClient()


        //Headers in the request do not work, We need to develope a CustomWebSocket
        // instead of RealWebSocket
        fun OkHttpClient.customNewWebSocket(
            request: Request,
            listener: WebSocketListener) : WebSocket
        {
            val webSocket = RealWebSocket(request, listener, Random(), pingIntervalMillis.toLong())
            webSocket.connect(this)
            return webSocket
        }



        binding.connectButton.setOnClickListener {
            val apiKey = "0E9D21AC-1CB5-4385-93BC-01AE14C1CCBD"
            val port = "8883"
            val url = "wss://www.getholler.com"
            val request : Request = Request
                .Builder()
                .url("$url:$port")
                .addHeader("Sec-WebSocket-Key", apiKey)
                .build()
            val deviceId = Settings.Secure.getString(
                this.contentResolver,
                Settings.Secure.ANDROID_ID)
            val listener = WebSocketListener(deviceId)
            val ws : WebSocket = client.newWebSocket(request, listener)
        }
    }
}