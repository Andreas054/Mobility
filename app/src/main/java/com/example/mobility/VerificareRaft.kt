package com.example.mobility

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
import androidx.appcompat.app.AlertDialog
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONTokener
import java.net.HttpURLConnection
import java.net.URL

class VerificareRaft : AppCompatActivity() {

    var artnrGLOBAL = 0
    var codprodusGLOBAL = "0"
    var codOK = false

    private lateinit var inputCodVerif: EditText
    private lateinit var textProdusNumeVerif: TextView
    private lateinit var textProdusPretVerif: TextView
    private lateinit var textProdusStocVerif: TextView
    private lateinit var buttonBackMainFromVerifRaft: Button
    private lateinit var buttonClearVerif: Button
    private lateinit var buttonAddEtichetare: Button

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

        // Set cursor on COD
        inputCodVerif.requestFocus()

        // Listen for the carriage return key(\r) on the inputCod editText from Scanner => send to the SERVER
        inputCodVerif.setOnKeyListener { v, keyCode, keyEvent ->
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
        inputCodVerif.setOnEditorActionListener { v, actionId, event ->
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
                if (codOK == true) {
                    GlobalScope.launch {
                        sendJsonAddEtichetare()
                    }
                    codOK = false
                }
                else {
                    Toast.makeText(
                        this@VerificareRaft,
                        "Cod Inexistent!",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
            else {
                Toast.makeText(
                    this@VerificareRaft,
                    "Codul nu poate fi NULL!",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        buttonClearVerif.setOnClickListener {
            ClearVerif()
        }

        configurebuttonBack()
    }

    private fun configurebuttonBack() {
        buttonBackMainFromVerifRaft.setOnClickListener {
            finish()
        }
    }

    private fun ClearVerif   () {
        // the CLEAR button sets everything to default and sets focus on the inputCod
        inputCodVerif.setText("")
        textProdusNumeVerif.setText("Articol")
        textProdusPretVerif.setText("Pret")
        textProdusStocVerif.setText("Stoc")
        inputCodVerif.requestFocus()
    }

    private fun sendCodOnEnter() {
        var codprodus = inputCodVerif.text.toString()
        if (codprodus.length == 0) {
            Toast.makeText(
                this@VerificareRaft,
                "Codul nu poate fi NULL!",
                Toast.LENGTH_SHORT
            ).show()
        }
        else {
            codprodusGLOBAL = codprodus
            // when is similitar to the switch
            when (codprodus.length) {
                8 -> codprodus = codprodus.dropLast(1)
                // If EAN-13 => cut the last letter
                13 -> codprodus = codprodus.dropLast(1)
            }

            GlobalScope.launch {
                getJsonArticolCurent(codprodus)
            }
        }
    }

    @UiThread
    // Get the NAME and PRICE of the current scanned product and send it to the SERVER
    suspend fun getJsonArticolCurent(codprodus: String)  {
        try {
            val url = URL("http://" + serverip + ":8001/produscurentpretstoc")
            var response: String
            response = ""
            with(url.openConnection() as HttpURLConnection) {
                // Set connection timeout and display TOAST message if server is not responding
                setConnectTimeout(timeoutconnection)

                requestMethod = "POST"  // optional default is GET

                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                doOutput

                // The JSON Request
                var jsonrequest = "{\"codprodus\": " + codprodus + "}"
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

                try {
                    // Convert the response string to a JSON array
                    val jsonArray = JSONTokener(response).nextValue() as JSONArray
                    // Convert the response string to a JSON array
                    // The SERVER should return the name and price of the product
                    val item = jsonArray.getJSONObject(0)
                    val numeprodus = item.getString("numeprodus")
                    val pretprodus = item.getString("pretprodus")
                    val artnr = item.getString("artnr")
                    val stoc = item.getString("stoc")
                    artnrGLOBAL = artnr.toInt()
                    // Update the name and price of the product on the display
                    runOnUiThread {
                        textProdusNumeVerif.setText(numeprodus)
                        textProdusPretVerif.setText(pretprodus + " Lei")
                        textProdusStocVerif.setText(stoc)
                        // In case of an error the SERVER sends the pretprodus as 0
                        // So if an error occurs dont re-enable the inputCantitate editText
                        if (pretprodus != "0") {
                            codOK = true
                            // Play NOTIFICATION SOUND if COD is valid
                            try {
                                val notification: Uri =
                                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                                val r = RingtoneManager.getRingtone(applicationContext, notification)
                                r.play()
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        } else {
                            codOK = false
                            Toast.makeText(
                                this@VerificareRaft,
                                "Cod Inexistent!",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                } catch (e: Exception) {
                    println(e)
                }
            }
        }
        catch (e: java.lang.Exception) {
            println(e)
            runOnUiThread {
                Toast.makeText(this@VerificareRaft, "Eroare comunicare SERVER!", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    suspend fun sendJsonAddEtichetare() {
        try {
            val url = URL("http://" + serverip + ":8001/addetichetare")
            var response: String
            response = ""
            with(url.openConnection() as HttpURLConnection) {
                // Set connection timeout and display TOAST message if server is not responding
                setConnectTimeout(timeoutconnection)

                requestMethod = "POST"  // optional default is GET

                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                doOutput

                // The JSON Request
                var jsonrequest =
                    "{\"artnr\": " + artnrGLOBAL + ",\"codprodus\": " + codprodusGLOBAL + "}"
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
                // The SERVER should return the success = True back to finish this activity
                val jsonArray = JSONTokener(response).nextValue() as JSONArray
                val item = jsonArray.getJSONObject((0))
                val success = item.getString("success")
                if (success.toBoolean() == true) {
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
                        ClearVerif()
                    }
                    delay(1000)
                }
            }
        } catch (e: java.lang.Exception) {
            println(e)
            runOnUiThread {
                Toast.makeText(this@VerificareRaft, "Eroare comunicare SERVER!", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

}