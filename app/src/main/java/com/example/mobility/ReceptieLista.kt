package com.example.mobility

import android.content.Intent
import android.media.MediaPlayer
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.Toast
import androidx.annotation.UiThread
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONTokener
import java.lang.Exception
import java.net.HttpURLConnection
import java.net.URL

class ReceptieLista : AppCompatActivity() {
    private var furnizorCurentNume = "Furnizor"

    // The listView with all the FURNIZORI and their IDs
    private var listaMutableFurnizoriNume = mutableListOf("No data")
    private var listaMutableFurnizoriID = mutableListOf(0)


    private lateinit var listviewlistaFurnizori: ListView
    private lateinit var buttonGetListaFurnizori: Button
    private lateinit var buttonBackMain: Button

    private lateinit var arrayAdapterStorageListaFurnizori: ArrayAdapter<String>

    private fun sunetErrorMajor() {
        // Play custom sound to notify Major Error
        MediaPlayer.create(this@ReceptieLista, R.raw.error_major).start()
    }

    private fun sunetErrorMinor() {
        // Play custom sound to notify Minor Error
        MediaPlayer.create(this@ReceptieLista, R.raw.error_minor).start()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_receptie_lista)

        listviewlistaFurnizori  = findViewById(R.id.listviewlistaFurnizori)
        buttonGetListaFurnizori  = findViewById(R.id.buttonGetListaFurnizori)
        buttonBackMain  = findViewById(R.id.buttonBackMain)

        GlobalScope.launch {
            getJsonListaFurnizori()
        }

        configurebuttonBack()

        buttonGetListaFurnizori.setOnClickListener {
            GlobalScope.launch {
                getJsonListaFurnizori()
            }
        }

        arrayAdapterStorageListaFurnizori = ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, listaMutableFurnizoriNume)
        listviewlistaFurnizori.adapter = arrayAdapterStorageListaFurnizori

        listviewlistaFurnizori.setOnItemClickListener { parent, _, position, _ ->
            // Save into global variable the current furnizor NAME
            furnizorCurentNume = parent.getItemAtPosition(position).toString()  // The item that was clicked
            if (furnizorCurentNume == "No data") {
                sunetErrorMinor()
                runOnUiThread {
                    Toast.makeText(
                        this@ReceptieLista,
                        "Incarca lista Furnizori!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } else {
                val intent = Intent(this@ReceptieLista, ReceptieFurnizorMain::class.java)
                intent.putExtra("furnizorCurentNume", furnizorCurentNume)
                intent.putExtra("furnizorCurentID", listaMutableFurnizoriID[position + 1])
                startActivity(intent)
            }
        }
    }

    private fun configurebuttonBack() {
        buttonBackMain.setOnClickListener {
            finish()
        }
    }

    @UiThread
    fun getJsonListaFurnizori()  {
        try {
            val url = URL("http://$serverip:8001/furnizori")
            var response = ""
            with(url.openConnection() as HttpURLConnection) {
                // Set connection timeout and display TOAST message if server is not responding
                connectTimeout = timeoutconnection
                requestMethod = "GET"  // optional default is GET
                println("\nSent 'GET' request to URL : $url; Response Code : $responseCode")

                // Save response from HTTP GET request to string response
                inputStream.bufferedReader().use {
                    it.lines().forEach { line ->
                        println(line)
                        response += line
                    }
                }

                // Convert the response string to a JSON array
                val jsonArray = JSONTokener(response).nextValue() as JSONArray

                // Clear the list before updating it, afterwards add all FURNIZOR names and FURNIZOR IDs
                listaMutableFurnizoriNume.clear()
                for (i in 0 until jsonArray.length()) {
                    val item = jsonArray.getJSONObject(i)
                    val id = item.getInt("id")
                    val nume = item.getString("nume")
                    listaMutableFurnizoriNume.add(nume)
                    listaMutableFurnizoriID.add(id)
                }

                // runOnUiThread to update the Screen with new values
                runOnUiThread {
                    arrayAdapterStorageListaFurnizori.notifyDataSetChanged()
                }
            }
        }
        catch (e: Exception) {
            println(e)
            sunetErrorMajor()
            runOnUiThread {
                Toast.makeText(this@ReceptieLista, "Eroare comunicare SERVER!", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }
}