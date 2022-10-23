package com.example.mobility

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast

class Settings : AppCompatActivity() {
    private lateinit var buttonBackMainFromSettings: Button
    private lateinit var buttonSettingsApply: Button
    private lateinit var inputServerIP: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        val sharedPref = getSharedPreferences("ipAddress", Context.MODE_PRIVATE)
        var edit = sharedPref.edit()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        buttonBackMainFromSettings  = findViewById(R.id.buttonBackMainFromSettings)
        buttonSettingsApply  = findViewById(R.id.buttonSettingsApply)
        inputServerIP  = findViewById(R.id.inputServerIP)

        // Set serverip from cfg file on phone (sharedPref)
        //serverip = sharedPref.getString("serverip", "default value").toString()
        inputServerIP.setText(serverip)

        configurebuttonBack()

        buttonSettingsApply.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View) {
                serverip = inputServerIP.text.toString()
                println(serverip)
                edit.putString("serverip", serverip)
                edit.commit()
                Toast.makeText(this@Settings,"Saved locally", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun configurebuttonBack() {
        buttonBackMainFromSettings.setOnClickListener {
            finish()
        }
    }
}