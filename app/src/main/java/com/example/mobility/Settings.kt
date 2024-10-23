package com.example.mobility

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast

const val timeoutconnection = 3000

var serverip = "172.16.0.154"

class Settings : AppCompatActivity() {
    private lateinit var buttonBackMainFromSettings: Button
    private lateinit var buttonSettingsApply: Button
    private lateinit var inputServerIP: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        val sharedPref = getSharedPreferences("ipAddress", Context.MODE_PRIVATE)
        val editSharedPref = sharedPref.edit()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        buttonBackMainFromSettings = findViewById(R.id.buttonBackMainFromSettings)
        buttonSettingsApply = findViewById(R.id.buttonSettingsApply)
        inputServerIP = findViewById(R.id.inputServerIP)

        // Set serverip from cfg file on phone (sharedPref)
        //serverip = sharedPref.getString("serverip", "default value").toString()
        inputServerIP.setText(serverip)


        buttonBackMainFromSettings.setOnClickListener {
            finish()
        }

        buttonSettingsApply.setOnClickListener {
            serverip = inputServerIP.text.toString()
            println(serverip)
            editSharedPref.putString("serverip", serverip)
            editSharedPref.apply()
            Toast.makeText(this@Settings, "Saved locally", Toast.LENGTH_SHORT).show()
        }
    }
}