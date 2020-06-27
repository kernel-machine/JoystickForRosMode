package com.example.joystick2

import JoyStick
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.io.PrintWriter
import java.io.Serializable
import java.net.Socket
import java.util.*
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin


class Axis : Serializable {
    var pitch: Int = 0
    var roll: Int = 0
    var throotle: Int = 0
    var yaw: Int = 0

    fun isFull(): Boolean {
        return pitch != 0 && roll != 0 && throotle != 0 && yaw != 0
    }

    fun erase() {
        pitch = 0
        roll = 0
        throotle = 0
        yaw = 0
    }
}

class MainActivity : AppCompatActivity(), JoyStick.JoyStickListener, View.OnClickListener {

    private lateinit var jsLeft: JoyStick
    private lateinit var jsRight: JoyStick
    private lateinit var socket: Socket
    private lateinit var printwriter: PrintWriter
    private lateinit var scanner: Scanner
    private var processNext = true
    private var axis = Axis()
    private var IsConnected = false

    private lateinit var ipaddrEditText: EditText
    private lateinit var connectBtn: Button
    private lateinit var portEditText: EditText
    private lateinit var stateText: TextView

    private lateinit var context: Context


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        context = this

        val layout = findViewById<LinearLayout>(R.id.layout)
        jsLeft = JoyStick(this)
        jsRight = JoyStick(this)

        jsLeft.setButtonColor(Color.BLUE)
        jsRight.setButtonColor(Color.GRAY)

        jsLeft.isShouldBeXcenter = false

        val size = 800
        jsLeft.layoutParams = ViewGroup.LayoutParams(size, size)
        jsRight.layoutParams = ViewGroup.LayoutParams(size, size)

        layout.addView(jsLeft)
        layout.addView(jsRight)

        jsRight.setListener(this)
        jsLeft.setListener(this)

        ipaddrEditText = findViewById(R.id.ipaddr)
        connectBtn = findViewById(R.id.connectButton)
        portEditText = findViewById(R.id.port)
        stateText = findViewById(R.id.state)

        val sharedPref = getSharedPreferences("js", Context.MODE_PRIVATE)
        ipaddrEditText.setText(sharedPref.getString("ip_addr", ""));
        portEditText.setText(sharedPref.getString("port", ""));

        connectBtn.setOnClickListener(this)
    }

    override fun onTap() {

    }

    override fun onDoubleTap() {

    }

    override fun onMove(joyStick: JoyStick?, angle: Double, power: Double, direction: Int) {

        if (!processNext) return;
        processNext = false;
        val angleDegreee: Double = 180.0 * (angle / 3.0) //Math.toDegrees(angle)

        val radians = Math.toRadians(angleDegreee)
        var x = power * sin(radians)
        var y = power * cos(radians)


        if (angleDegreee >= 0) x = abs(x)
        else x = -abs(x)

        if (angleDegreee >= 90 || angleDegreee <= -90)
            y = abs(y)
        else
            y = -abs(y)

        if (joyStick == jsLeft) {
            axis.yaw = (y * 100.0).toInt()
            axis.throotle = (x * 100).toInt()
        }
        if (joyStick == jsRight) {
            axis.pitch = (x * 100).toInt()
            axis.roll = (y * 100).toInt()
        }

        if (IsConnected) {
            val t = Thread {
                val s =
                    "|p:" + axis.pitch + "r:" + axis.roll + "t:" + axis.throotle + "y:" + axis.yaw
                Log.d("SNEDED", s)
                printwriter.print(s)
                printwriter.flush()
                if (printwriter.checkError()) {
                    closeSocket()
                }

            }
            t.start()
            t.join()
        }

        processNext = true
    }

    override fun onClick(v: View?) {
        if (IsConnected) {
            closeSocket()
            return
        }

        val add = ipaddrEditText.text.toString()
        val portNumber = Integer.parseInt(portEditText.text.toString())
        val t = Thread {
            try {
                socket = Socket(add, portNumber)
                printwriter = PrintWriter(socket.getOutputStream());
                scanner = Scanner(socket.getInputStream())
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(context, "Cannot connect", Toast.LENGTH_SHORT).show()
                }
            }


            if (this::socket.isInitialized && socket.isConnected) {
                runOnUiThread {
                    IsConnected = true
                    connectBtn.setText(R.string.disconnect)
                    stateText.setText(R.string.connected)
                    Toast.makeText(this, R.string.connected, Toast.LENGTH_SHORT).show()
                }
            }
        }
        t.start()
        t.join()

        val sharedPref = getSharedPreferences("js", Context.MODE_PRIVATE)
        val editor = sharedPref.edit()
        editor.putString("ip_addr", add)
        editor.putString("port", portNumber.toString())
        editor.apply()
    }

    fun closeSocket() {
        val t = Thread {
            socket.close()
        }
        t.start()
        t.join()
        runOnUiThread {
            connectBtn.setText(R.string.connect)
            stateText.setText(R.string.disconnected)
        }
        runOnUiThread {
            Toast.makeText(this, R.string.disconnected, Toast.LENGTH_SHORT).show()
        }
        IsConnected = false
    }
}
