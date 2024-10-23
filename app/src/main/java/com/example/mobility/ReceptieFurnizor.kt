package com.example.mobility

import android.content.Intent
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.UiThread
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONTokener
import java.net.*


class ReceptieFurnizor : AppCompatActivity() {
//    furnizorCurentNume & docnr & idrec from ReceptieFurnizorMain
    private var furnizorCurentNume = "Furnizor"

    private var docnr = 1L // Nr Factura
    private var idrec = 0



    private var artnr = 0
    // How many items have been sent to the server
    private var cantitateReceptieNR = 0
    // The total of everything that has been sent to the server
    private var cantitateReceptieTotal = 0.0

    private lateinit var inputCod: EditText
    private lateinit var inputCantitate: EditText
    private lateinit var textProdusNume: TextView
    private lateinit var textProdusPret: TextView
    private lateinit var textDocNr: TextView
    private lateinit var textFurnizorCurentReceptie: TextView
    private lateinit var textNrProduseNR: TextView
    private lateinit var textCantitateTotalaNR: TextView
    private lateinit var buttonEmiteReceptie: Button
    private lateinit var buttonClearReceptie: Button
    private lateinit var buttonBackRecMain: Button

    private fun sunetErrorMajor() {
        // Play custom sound to notify Major Error
        MediaPlayer.create(this@ReceptieFurnizor, R.raw.error_major).start()
    }

    private fun sunetErrorMinor() {
        // Play custom sound to notify Minor Error
        MediaPlayer.create(this@ReceptieFurnizor, R.raw.error_minor).start()
    }

    private fun sunetClear() {
        // Play custom sound to notify Clear text
        MediaPlayer.create(this@ReceptieFurnizor, R.raw.clear).start()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_receptie_furnizor)

        inputCod = findViewById(R.id.inputCod)
        inputCantitate = findViewById(R.id.inputCantitate)
        textProdusNume = findViewById(R.id.textProdusNume)
        textProdusPret = findViewById(R.id.textProdusPret)
        textDocNr = findViewById(R.id.textDocNr)
        textFurnizorCurentReceptie = findViewById(R.id.textFurnizorCurentReceptie)
        textNrProduseNR = findViewById(R.id.textNrProduseNR)
        textCantitateTotalaNR = findViewById(R.id.textCantitateTotalaNR)
        buttonEmiteReceptie = findViewById(R.id.buttonEmiteReceptie)
        buttonClearReceptie = findViewById(R.id.buttonClearReceptie)
        buttonBackRecMain  = findViewById(R.id.buttonBackRecMain)

        val bundle = intent.extras!!
        furnizorCurentNume = bundle.getString("furnizorCurentNume")!!
        docnr = bundle.getLong("docnr")
        idrec = bundle.getInt("idrec")


        // Set cursor on COD
        inputCod.requestFocus()
        // Set the DocNR text to NR FACTURA
        textDocNr.text = docnr.toString()
        // Set the Top text to the current FURNIZOR NAME
        textFurnizorCurentReceptie.text = furnizorCurentNume
        // Disable the CANTITATE INPUT by default
        inputCantitate.isEnabled = false

        GlobalScope.launch {
            getJsonCountCantitate()
        }

        configurebuttonClearReceptie()
        configurebuttonBack()

        // Listen for the carriage return key(\r) on the inputCod editText from Scanner => send to the SERVER
        inputCod.setOnKeyListener { _, keyCode, keyEvent ->
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
        inputCod.setOnEditorActionListener { _, actionId, _ ->
            return@setOnEditorActionListener when (actionId) {
                EditorInfo.IME_ACTION_SEND -> {
                    sendCodOnEnter()
                    true
                }
                else -> false
            }
        }

        // Listen for the send key(ENTER) on the inputCantitate editText => send to the SERVER along with the COD
        inputCantitate.setOnEditorActionListener { _, actionId, _ ->
            return@setOnEditorActionListener when (actionId) {
                EditorInfo.IME_ACTION_SEND -> {
                    val codEANProdus = inputCod.text.toString()
                    val cantitateprodus = inputCantitate.text.toString()
                    // Check if COD is NULL
                    if (codEANProdus.isEmpty()) {
                        sunetErrorMinor()
                        runOnUiThread {
                            Toast.makeText(
                                this@ReceptieFurnizor,
                                "Codul nu poate fi NULL!",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        if (codEANProdus.startsWith("0")) {
                            sunetErrorMinor()
                            runOnUiThread {
                                Toast.makeText(
                                    this@ReceptieFurnizor,
                                    "Codul nu poate incepe cu 0!",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                        else {
                            // Check if CANTITATE is NULL
                            if (cantitateprodus.isEmpty() or cantitateprodus.startsWith("0")) {
                                sunetErrorMinor()
                                runOnUiThread {
                                    Toast.makeText(
                                        this@ReceptieFurnizor,
                                        "Cantitatea nu poate fi NULL!",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                            // If COD and CANTITATE are NOT NULL add 1 to the How many items in receptie and add the total to the total of everything
                            // Afterwards send COD and CANTITATE to the SERVER
                            else {
                                cantitateReceptieNR++
                                cantitateReceptieTotal += inputCantitate.text.toString().toInt()
                                GlobalScope.launch {
                                    sendJsonArticolCurent()
                                }
                            }
                        }
                    }
                    true
                }
                else -> false
            }
        }

        buttonEmiteReceptie.setOnClickListener {
            val builder = AlertDialog.Builder(this@ReceptieFurnizor)
            if (cantitateReceptieTotal > 0) {
                builder.setMessage("Finalizare receptie?")
                    .setCancelable(false)
                    .setPositiveButton("Da") { _, _ ->
                        GlobalScope.launch {
                            sendJsonEmiteReceptie()
                        }
                    }
                    .setNegativeButton("Nu") { dialog, _ ->
                        // Dismiss the dialog
                        dialog.dismiss()
                    }
                val alert = builder.create()
                alert.show()
            }
            else {
                sunetErrorMinor()
                runOnUiThread {
                    Toast.makeText(
                        this@ReceptieFurnizor,
                        "Receptie nu poate fi goala!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    // On back button press go back to Lista Furnizori
    private var backPressedTime = 0L

    override fun onBackPressed() {
        if (backPressedTime + 2000 > System.currentTimeMillis()) {
            finish()
            val intent = Intent(applicationContext, ReceptieLista::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(intent)
        } else {
            runOnUiThread {
                Toast.makeText(this, "Apasa din nou pentru a te intoarce", Toast.LENGTH_LONG).show()
            }
        }
        backPressedTime = System.currentTimeMillis()
    }

    private fun sendCodOnEnter() {
        val codEANProdus = inputCod.text.toString()
        if (codEANProdus.isEmpty()) {
            sunetErrorMinor()
            runOnUiThread {
                Toast.makeText(
                    this@ReceptieFurnizor,
                    "Codul nu poate fi NULL!",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } else {
            if (codEANProdus.startsWith("0")) {
                sunetErrorMinor()
                runOnUiThread {
                    Toast.makeText(
                        this@ReceptieFurnizor,
                        "Codul nu poate incepe cu 0!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } else {
                GlobalScope.launch {
                    getJsonArticolCurent(codEANProdus)
                }
            }
        }
    }

    private fun functieClearReceptie() {
        // the CLEAR button sets everything to default and sets focus on the inputCod
        inputCod.setText("")
        inputCantitate.setText("")
        textProdusNume.text = "Articol"
        textProdusPret.text = "Pret"
        inputCod.requestFocus()
        inputCod.isEnabled = true
        inputCantitate.isEnabled = false
    }

    private fun configurebuttonClearReceptie() {
        buttonClearReceptie.setOnClickListener {
            sunetClear()
           functieClearReceptie()
        }
    }

    private fun configurebuttonBack() {
        buttonBackRecMain.setOnClickListener {
            finish()
            val intent = Intent(applicationContext, ReceptieLista::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(intent)
        }
    }

    @UiThread
    // Get the NAME and PRICE of the current scanned product and send it to the SERVER
    fun getJsonArticolCurent(codEANProdus: String)  {
        try {
            val url = URL("http://$serverip:8001/produscurentpret")
            var response = ""
            with(url.openConnection() as HttpURLConnection) {
                // Set connection timeout and display TOAST message if server is not responding
                connectTimeout = timeoutconnection
                requestMethod = "POST"  // optional default is GET
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
                    val numeprodus = item.getString("numeprodus")
                    val pretprodus = item.getDouble("pretprodus")
                    artnr = item.getInt("artnr")
                    // Update the name and price of the product on the display
                    runOnUiThread {
                        textProdusNume.text = numeprodus
                        textProdusPret.text = "$pretprodus Lei"
                        // In case of an error the SERVER sends the pretprodus as 0
                        // So if an error occurs dont re-enable the inputCantitate editText
                        inputCod.isEnabled = false
                        inputCantitate.isEnabled = true
                        inputCantitate.requestFocus()
                        // Play NOTIFICATION SOUND if COD is valid
                        try {
                            val notification: Uri =
                                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                            val r = RingtoneManager.getRingtone(
                                applicationContext,
                                notification
                            )
                            r.play()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                } catch (e : JSONException) {
//                        ARTNR is NULL (nu exista cod EAN)
                    sunetErrorMinor()
                    runOnUiThread {
                        Toast.makeText(
                            this@ReceptieFurnizor,
                            "Cod Inexistent!",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
        catch (e: java.lang.Exception) {
            println(e)
            sunetErrorMajor()
            runOnUiThread {
                Toast.makeText(this@ReceptieFurnizor, "Eroare comunicare SERVER!", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    // Get the current CantitateReceptie and CountReceptie
    private fun getJsonCountCantitate()  {
        try {
            val url = URL("http://$serverip:8001/cantitatereceptie")
            var response = ""
            with(url.openConnection() as HttpURLConnection) {
                // Set connection timeout and display TOAST message if server is not responding
                connectTimeout = timeoutconnection
                requestMethod = "POST"  // optional default is GET
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                doOutput

                // The JSON Request
                val jsonrequest = "{\"idrec\": $idrec}"
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

                try {
                    // Convert the response string to a JSON array
                    val jsonArray = JSONTokener(response).nextValue() as JSONArray
                    // Convert the response string to a JSON array
                    // The SERVER should return the name and price of the product
                    val item = jsonArray.getJSONObject(0)
                    val countreceptie = item.getInt("countreceptie")
                    val cantitatereceptie = item.getDouble("cantitatereceptie")
                    println(countreceptie)
                    println(cantitatereceptie)
                    // Update the name and price of the product on the display
                    cantitateReceptieNR = countreceptie
                    cantitateReceptieTotal = cantitatereceptie
                    runOnUiThread {
                        textNrProduseNR.text = cantitateReceptieNR.toString()
                        textCantitateTotalaNR.text = cantitateReceptieTotal.toString()
                    }
                } catch (e: Exception) {
                    println(e)
                }
            }
        }
        catch (e: java.lang.Exception) {
            println(e)
            sunetErrorMajor()
            runOnUiThread {
                Toast.makeText(this@ReceptieFurnizor, "Eroare comunicare SERVER!", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    // Send the COD and CANTITATE to the SERVER to add to the DATABASE
    private fun sendJsonArticolCurent() {
        try {
            val url = URL("http://$serverip:8001/produscurent")
            var response = ""
            with(url.openConnection() as HttpURLConnection) {
                // Set connection timeout and display TOAST message if server is not responding
                connectTimeout = timeoutconnection
                requestMethod = "POST"  // optional default is GET
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                doOutput

                // The JSON Request
                val cantitate = inputCantitate.text
                val jsonrequest = "{\"idrec\": $idrec,\"artnr\": $artnr,\"docnr\": $docnr,\"cantitate\": $cantitate}"
                println(jsonrequest)

                outputStream.write(jsonrequest.toByteArray(Charsets.UTF_8))
                println("\nSent 'POST' request to URL : $url; Response Code : $responseCode")

                // Save response from HTTP GET request to string response
                inputStream.bufferedReader().use {
                    it.lines().forEach { line ->
                        println(line)
                        response += line
                    }
                }

                // Convert the response string to a JSON array
                val jsonArray = JSONTokener(response).nextValue() as JSONArray
                val item = jsonArray.getJSONObject((0))
                val success = item.getBoolean("success")
                // The SERVER should send back success = True to indicate that it got the COD and CANTITATE, and play a NOTIFICATION SOUND
                if (success) {
                    try {
                        val notification: Uri =
                            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                        val r = RingtoneManager.getRingtone(applicationContext, notification)
                        r.play()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                // Set the text on screen to default and update the How many items in the receptie and total of everything

                runOnUiThread {
                    functieClearReceptie()
                    textNrProduseNR.text = cantitateReceptieNR.toString()
                    textCantitateTotalaNR.text = cantitateReceptieTotal.toString()
                }
            }
        }
        catch (e: java.lang.Exception) {
            println(e)
            sunetErrorMajor()
            runOnUiThread {
                Toast.makeText(this@ReceptieFurnizor, "Eroare comunicare SERVER!", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    private suspend fun sendJsonEmiteReceptie() {
        try {
            val url = URL("http://$serverip:8001/emitereceptie")
            var response = ""
            with(url.openConnection() as HttpURLConnection) {
                // Set connection timeout and display TOAST message if server is not responding
                connectTimeout = timeoutconnection
                requestMethod = "POST"  // optional default is GET
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                doOutput

                // The JSON Request
                val jsonrequest = "{\"idrec\": $idrec}"
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
                val success = item.getBoolean("success")
                if (success) {
                    runOnUiThread {
                        Toast.makeText(
                            this@ReceptieFurnizor,
                            "Receptie Finalizata!",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    delay(1000)
                    finish()
                    val intent = Intent(applicationContext, ReceptieLista::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    startActivity(intent)
                }
            }
        }
        catch (e: java.lang.Exception) {
            println(e)
            sunetErrorMajor()
            runOnUiThread {
                Toast.makeText(this@ReceptieFurnizor, "Eroare comunicare SERVER!", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }
}