package com.example.mobility

import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.UiThread
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONTokener
import java.net.HttpURLConnection
import java.net.URL

class ReceptieFurnizorMain : AppCompatActivity() {
//    furnizorCurentNume & furnizorCurentID from ReceptieLista
    private var furnizorCurentNume = "Furnizor"
    private var furnizorCurentID = 0

    private var docnr = 1L // Nr Factura

    // The listView with all the unfinished (STARE = 0) furnizori, receptii
    private var listaFurnizoriRecInLucru = mutableListOf("No data")
    private var listaDOCNRRecInLucru = mutableListOf(0L)

    private lateinit var textFurnizorCurrent: TextView
    private lateinit var editTextDoc: EditText
    private lateinit var buttonRecNou: Button
    private lateinit var buttonRecInLucru: Button
    private lateinit var listviewRecInLucru: ListView
    private lateinit var buttonBackRecList: Button

    private lateinit var arrayAdapterStorageReceptiiLucru: ArrayAdapter<String>
    
    private fun sunetErrorMajor() {
        // Play custom sound to notify Major Error
        MediaPlayer.create(this@ReceptieFurnizorMain, R.raw.error_major).start()
    }

    private fun sunetErrorMinor() {
        // Play custom sound to notify Minor Error
        MediaPlayer.create(this@ReceptieFurnizorMain, R.raw.error_minor).start()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_receptie_furnizor_main)

        textFurnizorCurrent  = findViewById(R.id.textFurnizorCurrent)
        editTextDoc = findViewById(R.id.editTextDoc)
        buttonRecNou = findViewById(R.id.buttonRecNou)
        buttonRecInLucru = findViewById(R.id.buttonRecInLucru)
        listviewRecInLucru = findViewById(R.id.listviewRecInLucru)
        buttonBackRecList  = findViewById(R.id.buttonBackRecList)

        val bundle = intent.extras!!
        furnizorCurentNume = bundle.getString("furnizorCurentNume")!!
        furnizorCurentID = bundle.getInt("furnizorCurentID")

        configurebuttonBackRecList()

        GlobalScope.launch {
            getJsonRecInLucru(furnizorCurentID)
        }

        // Set top text to current FURNIZOR
        textFurnizorCurrent.text = furnizorCurentNume

        // When Apply button is clicked get the docnr (NR FACTURA)
        buttonRecNou.setOnClickListener {
            try {
                docnr = editTextDoc.text.toString().toLong()

                if (docnr == 0L) {
                    sunetErrorMinor()
                    runOnUiThread {
                        Toast.makeText(
                            this@ReceptieFurnizorMain,
                            "Numar Receptie Invalid!",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    GlobalScope.launch {
                        sendJsonDocNr(docnr, true)
                    }
                }
            } catch (e: Exception) {
                println(e)
                sunetErrorMajor()
                runOnUiThread {
                    Toast.makeText(
                        this@ReceptieFurnizorMain,
                        "Numar Receptie Invalid!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        // Get the listView with all the unfinished (STARE=0) furnizori, receptii from the SERVER
        buttonRecInLucru.setOnClickListener {
            try {
                GlobalScope.launch {
                    getJsonRecInLucru(furnizorCurentID)
                }
            } catch (e: Exception) {
                println(e)
                sunetErrorMajor()
                runOnUiThread {
                    Toast.makeText(
                        this@ReceptieFurnizorMain,
                        "Numar Receptie Invalid!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        val mListView = findViewById<ListView>(R.id.listviewRecInLucru)
        arrayAdapterStorageReceptiiLucru = ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, listaFurnizoriRecInLucru)
        mListView.adapter = arrayAdapterStorageReceptiiLucru

        // If the listView with the unfinished furnizori, receptii is clicked start the next activity with the selected docnr
        listviewRecInLucru.setOnItemClickListener { parent, _, position, _ ->
            val docnrcurent = parent.getItemAtPosition(position).toString()  // The item that was clicked
            if (docnrcurent == "No data") {
                sunetErrorMinor()
                runOnUiThread {
                    Toast.makeText(
                        this@ReceptieFurnizorMain,
                        "Incarca Receptii in Lucru!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            else {
                docnr = listaDOCNRRecInLucru[position]
                GlobalScope.launch {
                    sendJsonDocNr(docnr, false)
                }
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
    fun getJsonRecInLucru(idfurn: Int) {
        try {
            val url = URL("http://$serverip:8001/receptiiinlucru")
            var response = ""
            with(url.openConnection() as HttpURLConnection) {
                // Set connection timeout and display TOAST message if server is not responding
                connectTimeout = timeoutconnection
                requestMethod = "POST"  // optional default is GET
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                doOutput

                // The JSON Request
                val jsonrequest = "{\"idfurn\": $idfurn}"
                println(jsonrequest)

                outputStream.write(jsonrequest.toByteArray(Charsets.UTF_8))
                println("\nSent 'POST' request to URL : $url; Response Code : $responseCode")

                // Save response from HTTP POST request to string response
                inputStream.bufferedReader().use {
                    it.lines().forEach { line ->
                        println(line)
                        response += line
                    }
                }

                // Clear the list before updating it, afterwards add all unfinished (STARE=0) furnizori, receptii from the SERVER
                listaFurnizoriRecInLucru.clear()
                listaDOCNRRecInLucru.clear()
                try {
                    // Convert the response string to a JSON array
                    val jsonArray = JSONTokener(response).nextValue() as JSONArray
                    // Go through every JSON object and add the docnr to the LISTVIEW
                    for (i in 0 until jsonArray.length()) {
                        val item = jsonArray.getJSONObject(i)
                        val docnr = item.getLong("doc")
                        val cantitatetotala = item.getDouble("cantitatetotala")
                        if (docnr != 0L) {
                            listaFurnizoriRecInLucru.add("Factura: $docnr , cantitate totala: $cantitatetotala")
                            listaDOCNRRecInLucru.add(docnr)
                        }
                    }
                } catch (e: Exception) {
                    println(e)
                }

                // Update the unfinished receptii LISTVIEW
                runOnUiThread {
                    arrayAdapterStorageReceptiiLucru.notifyDataSetChanged()
                }
            }
        }
        catch (e: Exception) {
            println(e)

            sunetErrorMajor()

            runOnUiThread {
                Toast.makeText(this@ReceptieFurnizorMain, "Eroare comunicare SERVER!", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    // Send to the SERVER the docnr(NR FACTURA) and the IDFURN for the current Receptie to be added in the DATABASE
    private fun sendJsonDocNr(docnr: Long, boolNewReceptie: Boolean)  {
        try {
            val url = URL("http://$serverip:8001/mobreceptieheader")
            var response = ""
            with(url.openConnection() as HttpURLConnection) {
                // Set connection timeout and display TOAST message if server is not responding
                connectTimeout = timeoutconnection
                requestMethod = "POST"  // optional default is GET
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                doOutput

                // The JSON Request
                val jsonrequest = "{\"boolNewReceptie\": $boolNewReceptie,\"doc\":$docnr,\"idfurn\": $furnizorCurentID}"
                println(jsonrequest)

                outputStream.write(jsonrequest.toByteArray(Charsets.UTF_8))
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
                val success = item.getBoolean("success")
                val idrec = item.getInt("idrec")
                if (success) {
                    val intent = Intent(this@ReceptieFurnizorMain, ReceptieFurnizor::class.java)
                    intent.putExtra("furnizorCurentNume", furnizorCurentNume)
                    intent.putExtra("docnr", docnr)
                    intent.putExtra("idrec", idrec)
                    startActivity(intent)
                }
                else {
                    sunetErrorMinor()

                    runOnUiThread {
                        Toast.makeText(
                            this@ReceptieFurnizorMain,
                            "Exista deja o receptie cu nr. $docnr!",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
        catch (e: Exception) {
            println(e)

            sunetErrorMajor()

            runOnUiThread {
                Toast.makeText(this@ReceptieFurnizorMain, "Eroare comunicare SERVER!", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }
}