package com.example.mobility

import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.UiThread
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONTokener
import java.net.HttpURLConnection
import java.net.URL

class VerificareRaft : AppCompatActivity() {
    private var artNrProdus = 0
    private var codEANProdus = "0"
    private var codEanIsValid = false

    private lateinit var inputCodVerif: EditText
    private lateinit var textProdusNumeVerif: TextView
    private lateinit var textProdusPretVerif: TextView
    private lateinit var textProdusStocVerif: TextView
    private lateinit var buttonBackMainFromVerifRaft: Button
    private lateinit var buttonClearVerif: Button
    private lateinit var buttonAddEtichetare: Button

    private fun sunetErrorMajor() {
        // Play custom sound to notify Major Error
        MediaPlayer.create(this@VerificareRaft, R.raw.error_major).start()
    }

    private fun sunetErrorMinor() {
        // Play custom sound to notify Minor Error
        MediaPlayer.create(this@VerificareRaft, R.raw.error_minor).start()
    }

    private fun sunetClear() {
        // Play custom sound to notify Clear text
        MediaPlayer.create(this@VerificareRaft, R.raw.clear).start()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_verificare_raft)

        inputCodVerif  = findViewById(R.id.inputCodVerif)
        textProdusNumeVerif  = findViewById(R.id.textProdusNumeVerif)
        textProdusPretVerif  = findViewById(R.id.textProdusPretVerif)
        textProdusStocVerif  = findViewById(R.id.textProdusStocVerif)
        buttonBackMainFromVerifRaft  = findViewById(R.id.buttonBackMainFromVerifRaft)
        buttonClearVerif  = findViewById(R.id.buttonClearVerif)
        buttonAddEtichetare  = findViewById(R.id.buttonAddEtichetare)

        // Set cursor on COD EAN
        inputCodVerif.requestFocus()

        // Listen for the carriage return key(\r) on the inputCod editText from Scanner => send to the SERVER
        inputCodVerif.setOnKeyListener { _, keyCode, keyEvent ->
            // keycode 66 = Carriage Return
            if(keyCode == 66 && keyEvent.action == KeyEvent.ACTION_UP) {
                sendCodOnEnter()
                true
            }
            else {
                false
            }
        }

        // Listen for the send key(ENTER) on the inputCod editText => send to the SERVER
        inputCodVerif.setOnEditorActionListener { _, actionId, _ ->
            return@setOnEditorActionListener when (actionId) {
                EditorInfo.IME_ACTION_SEND -> {
                    sendCodOnEnter()
                    true
                }
                else -> false
            }
        }

        buttonAddEtichetare.setOnClickListener {
            if (inputCodVerif.length() != 0) {
                if (codEanIsValid) {
                    GlobalScope.launch {
                        sendJsonAddEtichetare()
                    }
                    codEanIsValid = false
                }
                else {
                    sunetErrorMinor()
                    runOnUiThread {
                        Toast.makeText(
                            this@VerificareRaft,
                            "Cod Inexistent!",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
            else {
                sunetErrorMinor()
                runOnUiThread {
                    Toast.makeText(
                        this@VerificareRaft,
                        "Codul nu poate fi NULL!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        buttonClearVerif.setOnClickListener {
            clearVerif()
        }

        configurebuttonBack()
    }

    private fun configurebuttonBack() {
        buttonBackMainFromVerifRaft.setOnClickListener {
            finish()
        }
    }

    private fun clearVerif() {
        sunetClear()
        // the CLEAR button sets everything to default and sets focus on the inputCod
        inputCodVerif.isEnabled = true
        inputCodVerif.setText("")
        textProdusNumeVerif.text = "Articol"
        textProdusPretVerif.text = "Pret"
        textProdusStocVerif.text = "Stoc"
        inputCodVerif.requestFocus()
    }

    private fun sendCodOnEnter() {
        codEANProdus = inputCodVerif.text.toString()
        if (codEANProdus.isEmpty()) {
            sunetErrorMinor()
            runOnUiThread {
                Toast.makeText(
                    this@VerificareRaft,
                    "Codul nu poate fi NULL!",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } else {
            if (codEANProdus.startsWith("0")) {
                sunetErrorMinor()
                runOnUiThread {
                    Toast.makeText(
                        this@VerificareRaft,
                        "Codul nu poate incepe cu 0!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            else {
                GlobalScope.launch {
                    getJsonArticolCurent()
                }
            }
        }
    }

    @UiThread
    // Get the NAME and PRICE of the current scanned product and send it to the SERVER
    fun getJsonArticolCurent()  {
        try {
            val url = URL("http://$serverip:8001/produscurentpretstoc")
            var response = ""
            with(url.openConnection() as HttpURLConnection) {
                // Set connection timeout and display TOAST message if server is not responding
                connectTimeout = timeoutconnection
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                doOutput

                // The JSON Request
                val jsonrequest = "{\"codprodus\": $codEANProdus}"
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
                val jsonArray = JSONTokener(response).nextValue() as JSONArray
                // Convert the response string to a JSON array
                // The SERVER should return the name and price of the product
                val item = jsonArray.getJSONObject(0)

                try {
                    artNrProdus = item.getInt("artnr")
                    val numeprodus = item.getString("numeprodus")
                    val pretprodus = item.getDouble("pretprodus")
                    val stoc = item.getDouble("stoc")

                    // Disable text box if Cod is valid
                    runOnUiThread {
                        textProdusNumeVerif.text = numeprodus
                        textProdusPretVerif.text = "$pretprodus Lei"
                        textProdusStocVerif.text = stoc.toString()
                        inputCodVerif.isEnabled = false
                    }
                    codEanIsValid = true
                    // Play NOTIFICATION SOUND if COD is valid
                    try {
                        val notification: Uri =
                            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                        val r =
                            RingtoneManager.getRingtone(applicationContext, notification)
                        r.play()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                } catch (e : JSONException) {
//                        ARTNR is NULL (nu exista cod EAN)
                    sunetErrorMinor()
                    codEanIsValid = false

                    runOnUiThread {
                        Toast.makeText(
                            this@VerificareRaft,
                            "Cod Inexistent!",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
        catch (e: java.lang.Exception) {
            println(e)
            sunetErrorMajor()
            runOnUiThread {
                Toast.makeText(this@VerificareRaft, "Eroare comunicare SERVER!", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    private suspend fun sendJsonAddEtichetare() {
        try {
            val url = URL("http://$serverip:8001/addetichetare")
            var response = ""
            with(url.openConnection() as HttpURLConnection) {
                // Set connection timeout and display TOAST message if server is not responding
                connectTimeout = timeoutconnection
                requestMethod = "POST"  // optional default is GET
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                doOutput

                // The JSON Request
                val jsonrequest = "{\"artnr\": $artNrProdus,\"codprodus\": $codEANProdus}"
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
                // The SERVER should return the success = True back to finish this activity
                val jsonArray = JSONTokener(response).nextValue() as JSONArray
                val item = jsonArray.getJSONObject((0))

                if (item.getBoolean("success")) {
                    runOnUiThread {
                        // Play NOTIFICATION SOUND if it worked
                        try {
                            val notification: Uri =
                                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                            val r = RingtoneManager.getRingtone(applicationContext, notification)
                            r.play()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        clearVerif()
                    }
                    delay(1000)
                }
            }
        } catch (e: java.lang.Exception) {
            println(e)
            sunetErrorMajor()
            runOnUiThread {
                Toast.makeText(this@VerificareRaft, "Eroare comunicare SERVER!", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }
}