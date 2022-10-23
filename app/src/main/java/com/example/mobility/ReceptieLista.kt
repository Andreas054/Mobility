package com.example.mobility

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.view.View
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

// Global variables for the current furnizor and its ID
public var furnizorcurent = "Furnizor"
public var idfurnizorcurent = 0

class ReceptieLista : AppCompatActivity() {
    // The listView with all the FURNIZORI and their IDs
    var FurnizoriLista = mutableListOf("No data")
    var IDFurnizoriLista = mutableListOf(0)

    private lateinit var listviewlistaFurnizori: ListView
    private lateinit var buttonGetListaFurnizori: Button
    private lateinit var buttonBackMain: Button

    lateinit var arrayAdapter: ArrayAdapter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_receptie_lista)

        listviewlistaFurnizori  = findViewById(R.id.listviewlistaFurnizori)
        buttonGetListaFurnizori  = findViewById(R.id.buttonGetListaFurnizori)
        buttonBackMain  = findViewById(R.id.buttonBackMain)

        configurebuttonBack()

        GlobalScope.launch {
            getJsonListaFurnizori()
        }

        buttonGetListaFurnizori.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View) {
                GlobalScope.launch {
                    getJsonListaFurnizori()
                }
            }
        })

        val mListView = findViewById<ListView>(R.id.listviewlistaFurnizori)
        arrayAdapter = ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, FurnizoriLista)
        mListView.adapter = arrayAdapter

        listviewlistaFurnizori.setOnItemClickListener { parent, view, position, id ->
            // Save into global variable the current furnizor NAME
            furnizorcurent = parent.getItemAtPosition(position).toString()  // The item that was clicked
            if (furnizorcurent.equals("No data")) {
                Toast.makeText(this@ReceptieLista, "Incarca lista Furnizori!", Toast.LENGTH_SHORT).show()
            }
            else {
                // Save into global variable the current furnizor ID
                idfurnizorcurent = IDFurnizoriLista.get(position + 1)
                val intent = Intent(this, ReceptieFurnizorMain::class.java)
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
    suspend fun getJsonListaFurnizori()  {
        try {
            val url = URL("http://" + serverip + ":8001/furnizori")
            var response: String
            response = ""
            with(url.openConnection() as HttpURLConnection) {
                // Set connection timeout and display TOAST message if server is not responding
                setConnectTimeout(timeoutconnection)

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
                FurnizoriLista.clear()
                //IDFurnizoriLista.clear()
                for (i in 0 until jsonArray.length()) {
                    val item = jsonArray.getJSONObject(i)
                    val id = item.getString("id").toInt()
                    val nume = item.getString("nume")
                    FurnizoriLista.add(nume)
                    IDFurnizoriLista.add(id)
                }

                // runOnUiThread to update the Screen with new values
                runOnUiThread {
                    arrayAdapter.notifyDataSetChanged()

                }
            }
        }
        catch (e: Exception) {
            println(e)
            runOnUiThread {
                Toast.makeText(this@ReceptieLista, "Eroare comunicare SERVER!", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }
}