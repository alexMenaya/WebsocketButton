package com.example.websocketsbutton

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.websocketsbutton.databinding.ActivityMainBinding
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val client = OkHttpClient()

        binding.connectButton.setOnClickListener {
            val apiKey : String? = "0E9D21AC-1CB5-4385-93BC-01AE14C1CCBD"
            val port: String? = "8883"
            val url = "wss://www.getholler.com:8883/?key=0E9D21AC-1CB5-4385-93BC-01AE14C1CCBD" // this has worked: "ws://websocket-echo.com"
            val request : Request = Request
                .Builder()
                .url("ws://websocket-echo.com")
                .build()

            val listener = WebSocketListener()
            val ws : WebSocket = client.newWebSocket(request, listener)
        }
    }
}