package com.example.mobility

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.annotation.UiThread
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONTokener
import java.lang.Exception
import java.net.HttpURLConnection
import java.net.URL
import java.util.*

// Global variable for the current docnr (NR FACTURA)
public var docnr = 1L
public var idrecGLOBAL = 0

class ReceptieFurnizorMain : AppCompatActivity() {
    // The listView with all the unfinished (STARE=0) furnizori, receptii
    var ListaFurnizoriRecInLucru = mutableListOf("No data")
    var ListaDOCNRRecInLucru = mutableListOf(0L)

    private lateinit var textFurnizorCurrent: TextView
    private lateinit var editTextDoc: EditText
    private lateinit var buttonRecNou: Button
    private lateinit var buttonRecInLucru: Button
    private lateinit var listviewRecInLucru: ListView
    private lateinit var buttonBackRecList: Button

    lateinit var arrayAdapter: ArrayAdapter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_receptie_furnizor_main)

        textFurnizorCurrent  = findViewById(R.id.textFurnizorCurrent)
        editTextDoc = findViewById(R.id.editTextDoc)
        buttonRecNou = findViewById(R.id.buttonRecNou)
        buttonRecInLucru = findViewById(R.id.buttonRecInLucru)
        listviewRecInLucru = findViewById(R.id.listviewRecInLucru)
        buttonBackRecList  = findViewById(R.id.buttonBackRecList)

        configurebuttonBackRecList()

        GlobalScope.launch {
            getJsonRecInLucru(idfurnizorcurent)
        }

        // Set top text to current FURNIZOR
        textFurnizorCurrent.text = furnizorcurent

        // When Apply button is clicked get the docnr (NR FACTURA)
        buttonRecNou.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View) {
                try {
                    docnr = editTextDoc.text.toString().toLong()

                    if (docnr == 0L) {
                        // NEED TO ADD IF NR FACUTRA ALREADY EXISTS !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
                        Toast.makeText(this@ReceptieFurnizorMain, "Numar Receptie Invalid!", Toast.LENGTH_SHORT).show()
                    }
                    else {
                        GlobalScope.launch {
                            sendJsonDocNr(docnr, true)
                        }
                    }
                }
                catch (e: Exception) {
                    println(e)
                    Toast.makeText(this@ReceptieFurnizorMain, "Numar Receptie Invalid!", Toast.LENGTH_SHORT).show()
                }
            }
        })

        // Get the listView with all the unfinished (STARE=0) furnizori, receptii from the SERVER
        buttonRecInLucru.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View) {
                try {
                    GlobalScope.launch {
                        getJsonRecInLucru(idfurnizorcurent)
                    }
                }
                catch (e: Exception) {
                    println(e)
                    Toast.makeText(this@ReceptieFurnizorMain, "Numar Receptie Invalid!", Toast.LENGTH_SHORT).show()
                }
            }
        })

        val mListView = findViewById<ListView>(R.id.listviewRecInLucru)
        arrayAdapter = ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, ListaFurnizoriRecInLucru)
        mListView.adapter = arrayAdapter

        // If the listView with the unfinished furnizori, receptii is clicked start the next activity with the selected docnr
        listviewRecInLucru.setOnItemClickListener { parent, view, position, id ->
            val docnrcurent = parent.getItemAtPosition(position).toString()  // The item that was clicked
            if (docnrcurent.equals("No data")) {
                Toast.makeText(this@ReceptieFurnizorMain, "Incarca Receptii in Lucru!", Toast.LENGTH_SHORT).show()
            }
            else {
                docnr = ListaDOCNRRecInLucru.get(position)
                GlobalScope.launch {
                    sendJsonDocNr(docnr, false)
                }
                //val intent = Intent(this, ReceptieFurnizor::class.java)
                //startActivity(intent)
            }
        }
    }

    private fun configurebuttonBackRecList() {
        buttonBackRecList.setOnClickListener {
            finish()
        }
    }

    @UiThread
    // Get the unfinished (STARE=0) furnizori, receptii from the SERVER with the current IDFURN
    suspend fun getJsonRecInLucru(idfurn: Int) {
        try {
            val url = URL("http://" + serverip + ":8001/receptiiinlucru")
            var response: String
            response = ""
            with(url.openConnection() as HttpURLConnection) {
                // Set connection timeout and display TOAST message if server is not responding
                setConnectTimeout(timeoutconnection)

                requestMethod = "POST"  // optional default is GET

                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                doOutput

                // The JSON Request
                var jsonrequest = "{\"idfurn\": " + idfurn + "}"
                println(jsonrequest)

                var os = outputStream
                var jsonrequestbytes = jsonrequest.toByteArray(Charsets.UTF_8)
                os.write(jsonrequestbytes)
                println("\nSent 'POST' request to URL : $url; Response Code : $responseCode")

                // Save response from HTTP POST request to string response
                inputStream.bufferedReader().use {
                    it.lines().forEach { line ->
                        println(line)
                        response += line
                    }
                }

                // Clear the list before updating it, afterwards add all unfinished (STARE=0) furnizori, receptii from the SERVER
                ListaFurnizoriRecInLucru.clear()
                ListaDOCNRRecInLucru.clear()
                try {
                    // Convert the response string to a JSON array
                    val jsonArray = JSONTokener(response).nextValue() as JSONArray
                    // Go through every JSON object and add the docnr to the LISTVIEW
                    for (i in 0 until jsonArray.length()) {
                        val item = jsonArray.getJSONObject(i)
                        val docnr = item.getString("doc")
                        val cantitatetotala = item.getString("cantitatetotala")
                        if (docnr.toLong() != 0L) {
                            ListaFurnizoriRecInLucru.add("Factura: " + docnr + " , cantitate totala: " + cantitatetotala.toFloat().toInt())
                            ListaDOCNRRecInLucru.add(docnr.toLong())
                        }
                    }
                } catch (e: Exception) {
                    println(e)
                }

                // Update the unfinished receptii LISTVIEW
                runOnUiThread {
                    arrayAdapter.notifyDataSetChanged()
                }
            }
        }
        catch (e: Exception) {
            println(e)
            runOnUiThread {
                Toast.makeText(this@ReceptieFurnizorMain, "Eroare comunicare SERVER!", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    // Send to the SERVER the docnr(NR FACTURA) and the IDFURN for the current Receptie to be added in the DATABASE
    suspend fun sendJsonDocNr(docnr: Long, boolNewReceptie: Boolean)  {
        try {
            val url = URL("http://" + serverip + ":8001/mobreceptieheader")
            var response: String
            response = ""
            with(url.openConnection() as HttpURLConnection) {
                // Set connection timeout and display TOAST message if server is not responding
                setConnectTimeout(timeoutconnection)

                requestMethod = "POST"  // optional default is GET

                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                doOutput

                // The JSON Request
                var jsonrequest = "{\"boolNewReceptie\": " + boolNewReceptie + ",\"doc\":" + docnr + ",\"idfurn\": " + idfurnizorcurent + "}"
                println(jsonrequest)

                var os = outputStream
                var jsonrequestbytes = jsonrequest.toByteArray(Charsets.UTF_8)
                os.write(jsonrequestbytes)
                println("\nSent 'POST' request to URL : $url; Response Code : $responseCode")

                // Save response from HTTP POST request to string response
                inputStream.bufferedReader().use {
                    it.lines().forEach { line ->
                        println(line)
                        response += line
                    }
                }

                // Convert the response string to a JSON array
                // The SERVER should return the success = True back to start the next activity
                val jsonArray = JSONTokener(response).nextValue() as JSONArray
                val item = jsonArray.getJSONObject((0))
                val success = item.getString("success")
                val idrec = item.getString("idrec")
                idrecGLOBAL = idrec.toInt()
                if (success.toBoolean() == true) {
                    startActivity(
                        Intent(
                            this@ReceptieFurnizorMain,
                            ReceptieFurnizor::class.java
                        )
                    )
                }
                else {
                    if (success.toBoolean() == false) {
                        runOnUiThread {
                            Toast.makeText(this@ReceptieFurnizorMain, "Exista deja o receptie cu nr. " + docnr.toString() + "!", Toast.LENGTH_SHORT)
                                .show()
                        }
                    }
                }
            }
        }
        catch (e: Exception) {
            println(e)
            runOnUiThread {
                Toast.makeText(this@ReceptieFurnizorMain, "Eroare comunicare SERVER!", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }
}