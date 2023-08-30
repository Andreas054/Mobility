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
import org.json.JSONTokener
import java.net.*


var ab = ""
class ReceptieFurnizor : AppCompatActivity() {
    // How many items have been sent to the server
    var CantitateReceptieNR = 0
    // The total of everything that has been sent to the server
    var CantitateReceptieTotal = 0
    var artnrGLOBAL = 0

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

    // Get the time so you can only scan once every n seconds
    var timestart=System.currentTimeMillis()

    fun sunet_error_major() {
        // Play custom sound to notify Major Error
        val sunet: MediaPlayer = MediaPlayer.create(this@ReceptieFurnizor, R.raw.error_major)
        sunet.start()
    }

    fun sunet_error_minor() {
        // Play custom sound to notify Minor Error
        val sunet: MediaPlayer = MediaPlayer.create(this@ReceptieFurnizor, R.raw.error_minor)
        sunet.start()
    }

    fun sunet_clear() {
        // Play custom sound to notify Clear text
        val sunet: MediaPlayer = MediaPlayer.create(this@ReceptieFurnizor, R.raw.clear)
        sunet.start()
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

        // Set cursor on COD
        inputCod.requestFocus()

        // Set the DocNR text to NR FACTURA
        textDocNr.setText(docnr.toString())
        // Set the Top text to the current FURNIZOR NAME
        textFurnizorCurentReceptie.setText(furnizorcurent)
        // Disable the CANTITATE INPUT by default
        inputCantitate.setEnabled(false)

        GlobalScope.launch {
            getJsonCountCantitate()
        }

        // Listen for the carriage return key(\r) on the inputCod editText from Scanner => send to the SERVER
        inputCod.setOnKeyListener { v, keyCode, keyEvent ->
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
        inputCod.setOnEditorActionListener { v, actionId, event ->
            return@setOnEditorActionListener when (actionId) {
                EditorInfo.IME_ACTION_SEND -> {
                    sendCodOnEnter()
                    true
                }
                else -> false
            }
        }

        // Listen for the send key(ENTER) on the inputCantitate editText => send to the SERVER along with the COD
        inputCantitate.setOnEditorActionListener { v, actionId, event ->
            return@setOnEditorActionListener when (actionId) {
                EditorInfo.IME_ACTION_SEND -> {
                    var codprodus = inputCod.text.toString()
                    var cantitateprodus = inputCantitate.text.toString()
                    // Check if COD is NULL
                    if (codprodus.length == 0) {
                        sunet_error_minor()
                        Toast.makeText(
                            this@ReceptieFurnizor,
                            "Codul nu poate fi NULL!",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    else {
                        // Check if CANTITATE is NULL
                        if (cantitateprodus.length == 0) {
                            sunet_error_minor()
                            Toast.makeText(
                                this@ReceptieFurnizor,
                                "Cantitatea nu poate fi NULL!",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        // If COD and CANTITATE are !NULL add 1 to the How many items in receptie and add the total to the total of everything
                        // Afterwards send COD and CANTITATE to the SERVER
                        else {
                            CantitateReceptieNR ++
                            CantitateReceptieTotal += inputCantitate.text.toString().toInt()
                            GlobalScope.launch {
                                sendJsonArticolCurent(inputCod.text.toString())
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
            if (CantitateReceptieTotal > 0) {
                builder.setMessage("Finalizare receptie?")
                    .setCancelable(false)
                    .setPositiveButton("Da") { dialog, id ->
                        GlobalScope.launch {
                            sendJsonEmiteReceptie()
                        }
                    }
                    .setNegativeButton("Nu") { dialog, id ->
                        // Dismiss the dialog
                        dialog.dismiss()
                    }
                val alert = builder.create()
                alert.show()
            }
            else {
                sunet_error_minor()
                Toast.makeText(
                    this@ReceptieFurnizor,
                    "Receptie nu poate fi goala!",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        configurebuttonClearReceptie()
        configurebuttonBack()

    }

    // On back button press go back to Lista Furnizori
    private var backPressedTime:Long = 0
    lateinit var backToast:Toast
    override fun onBackPressed() {
        backToast = Toast.makeText(this, "Apasa din nou pentru a te intoarce", Toast.LENGTH_LONG)
        if (backPressedTime + 2000 > System.currentTimeMillis()) {
            backToast.cancel()
            finish()
            val intent = Intent(applicationContext, ReceptieLista::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(intent)
        } else {
            backToast.show()
        }
        backPressedTime = System.currentTimeMillis()
    }

    private fun sendCodOnEnter() {
        var codprodus = inputCod.text.toString()
        if (codprodus.length == 0) {
            sunet_error_minor()
            Toast.makeText(
                this@ReceptieFurnizor,
                "Codul nu poate fi NULL!",
                Toast.LENGTH_SHORT
            ).show()
        }
        else {
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

    private fun functieClearReceptie() {
        // the CLEAR button sets everything to default and sets focus on the inputCod
        inputCod.setText("")
        inputCantitate.setText("")
        textProdusNume.setText("Articol")
        textProdusPret.setText("Pret")
        inputCod.requestFocus()
        inputCod.setEnabled(true)
        inputCantitate.setEnabled(false)
    }

    private fun configurebuttonClearReceptie() {
        buttonClearReceptie.setOnClickListener {
            sunet_clear()
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
    suspend fun getJsonArticolCurent(codprodus: String)  {
        try {
            val url = URL("http://" + serverip + ":8001/produscurentpret")
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
                    artnrGLOBAL = artnr.toInt()
                    // Update the name and price of the product on the display
                    runOnUiThread {
                        textProdusNume.setText(numeprodus)
                        textProdusPret.setText(pretprodus + " Lei")
                        // In case of an error the SERVER sends the pretprodus as 0
                        // So if an error occurs dont re-enable the inputCantitate editText
                        if (pretprodus != "0") {
                            inputCod.setEnabled(false)
                            inputCantitate.setEnabled(true)
                            inputCantitate.requestFocus()
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
                            sunet_error_minor()
                            Toast.makeText(
                                this@ReceptieFurnizor,
                                "Cod Inexistent!",
                                Toast.LENGTH_LONG
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
            sunet_error_major()
            runOnUiThread {
                Toast.makeText(this@ReceptieFurnizor, "Eroare comunicare SERVER!", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    // Get the current CantitateReceptie and CountReceptie
    suspend fun getJsonCountCantitate()  {
        try {
            val url = URL("http://" + serverip + ":8001/cantitatereceptie")
            var response: String
            response = ""
            with(url.openConnection() as HttpURLConnection) {
                // Set connection timeout and display TOAST message if server is not responding
                setConnectTimeout(timeoutconnection)

                requestMethod = "POST"  // optional default is GET

                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                doOutput

                // The JSON Request
                var jsonrequest = "{\"idrec\": " + idrecGLOBAL + "}"
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
                    val countreceptie = item.getString("countreceptie")
                    val cantitatereceptie = item.getString("cantitatereceptie")
                    println(countreceptie)
                    println(cantitatereceptie)
                    // Update the name and price of the product on the display
                    CantitateReceptieNR = countreceptie.toInt()
                    CantitateReceptieTotal = cantitatereceptie.toFloat().toInt()
                    runOnUiThread {
                        textNrProduseNR.setText(CantitateReceptieNR.toString())
                        textCantitateTotalaNR.setText(CantitateReceptieTotal.toString())
                    }
                } catch (e: Exception) {
                    println(e)
                }
            }
        }
        catch (e: java.lang.Exception) {
            println(e)
            sunet_error_major()
            runOnUiThread {
                Toast.makeText(this@ReceptieFurnizor, "Eroare comunicare SERVER!", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    // Send the COD and CANTITATE to the SERVER to add to the DATABASE
    suspend fun sendJsonArticolCurent(codprodus: String)  {
        try {
            val url = URL("http://" + serverip + ":8001/produscurent")
            var response: String
            response = ""
            with(url.openConnection() as HttpURLConnection) {
                // Set connection timeout and display TOAST message if server is not responding
                setConnectTimeout(timeoutconnection)

                requestMethod = "POST"  // optional default is GET

                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                doOutput

                // The JSON Request
                var jsonrequest = "{\"idrec\": " + idrecGLOBAL + ",\"artnr\": " + artnrGLOBAL + ",\"docnr\": " + docnr + ",\"cantitate\": " + inputCantitate.text + "}"
                println(jsonrequest)

                var os = outputStream
                var jsonrequestbytes = jsonrequest.toByteArray(Charsets.UTF_8)
                os.write(jsonrequestbytes)
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
                val success = item.getString("success")
                // The SERVER should send back success = True to indicate that it got the COD and CANTITATE, and play a NOTIFICATION SOUND
                if (success.toBoolean() == true) {
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

//// maybe use Clear function here?
                runOnUiThread {
//                    inputCod.setText("")
//                    inputCantitate.setText("")
//                    inputCod.setEnabled(true)
//                    inputCod.requestFocus()
//                    inputCantitate.setEnabled(false)
//                    textProdusNume.setText("Articol")
//                    textProdusPret.setText("Pret")
                    functieClearReceptie()
                    textNrProduseNR.setText(CantitateReceptieNR.toString())
                    textCantitateTotalaNR.setText(CantitateReceptieTotal.toString())
                }
            }
        }
        catch (e: java.lang.Exception) {
            println(e)
            sunet_error_major()
            runOnUiThread {
                Toast.makeText(this@ReceptieFurnizor, "Eroare comunicare SERVER!", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    suspend fun sendJsonEmiteReceptie() {
        try {
            val url = URL("http://" + serverip + ":8001/emitereceptie")
            var response: String
            response = ""
            with(url.openConnection() as HttpURLConnection) {
                // Set connection timeout and display TOAST message if server is not responding
                setConnectTimeout(timeoutconnection)

                requestMethod = "POST"  // optional default is GET

                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                doOutput

                // The JSON Request
                var jsonrequest = "{\"idrec\": " + idrecGLOBAL + "}"
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
            sunet_error_major()
            runOnUiThread {
                Toast.makeText(this@ReceptieFurnizor, "Eroare comunicare SERVER!", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }
}