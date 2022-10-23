package com.example.mobility

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button

public val timeoutconnection = 1000

public var serverip = "172.16.0.154"

class MainActivity : AppCompatActivity() {
    private lateinit var buttonRecFurn: Button
    private lateinit var buttonVerifRaft: Button
    private lateinit var buttonSettings: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        buttonRecFurn  = findViewById(R.id.buttonRecFurn)
        buttonVerifRaft  = findViewById(R.id.buttonVerifRaft)
        buttonSettings = findViewById(R.id.buttonSettings)

        val sharedPref = getSharedPreferences("ipAddress", Context.MODE_PRIVATE)

        serverip = sharedPref.getString("serverip", "default value").toString()

        configurebuttonRecFurn()
        configurebuttonVerifRaft()
        configurebuttonSettings()
    }

    private fun configurebuttonRecFurn() {
        buttonRecFurn.setOnClickListener {
            startActivity(
                Intent(
                    this@MainActivity,
                    ReceptieLista::class.java
                )
            )
        }
    }

    private fun configurebuttonVerifRaft() {
        buttonVerifRaft.setOnClickListener {
            startActivity(
                Intent(
                    this@MainActivity,
                    VerificareRaft::class.java
                )
            )
        }
    }

    private fun configurebuttonSettings() {
        buttonSettings.setOnClickListener {
            startActivity(
                Intent(
                    this@MainActivity,
                    Settings::class.java
                )
            )
        }
    }
}