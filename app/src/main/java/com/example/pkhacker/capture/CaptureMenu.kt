package com.example.pkhacker.capture

import android.app.ActionBar
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.pkhacker.BluetoothService
import com.example.pkhacker.MainView
import com.example.pkhacker.R
import java.lang.NullPointerException

class CaptureMenu : AppCompatActivity() {

    lateinit var viewModel : MainView

    //UI Views
    lateinit var pairedDisplay : LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_capture_menu)

        initializeViews()

        viewModel = ViewModelProvider(this).get(MainView()::class.java).also { viewModel ->
            viewModel.initViewModel(applicationContext)
        }

        initializeObservers()


    }

    //ON CREATE FUNCTIONS
    private fun initializeViews(){
        pairedDisplay = findViewById(R.id.pairedDisplay)
    }
    private fun initializeObservers(){
        viewModel.serverSocket.observe(this, Observer<BluetoothSocket>{ serverSocket ->
            Log.i("bruhbruhbruh", "SERVER SOCKET CONNECTED YEAH!!!!!!!!!!!!!!!!!!!!!!!!!!!")
            capture()
        })
        viewModel.bluetoothServiceConnected.observe(this, Observer<Boolean>{ connected ->
            if(connected){
                listenForClients()
                populatePairedDevices()
            }
        })
        viewModel.testNewActivity.observe(this, Observer<Boolean>{ test ->
            if(test){
                capture()
            }
        })
    }


    //listen for incoming connections
    private fun listenForClients(){
        viewModel.listenForConnections()
    }

    //populate UI screen
    private fun populatePairedDevices(){
        try{
            viewModel.pairedDevices()!!.forEach { device ->
                val deviceName = device.name
                val deviceAddress = device.address // MAC address
                addButton(deviceName, deviceAddress)
            }
        }catch (err : NullPointerException){

        }

    }
    private fun addButton(name : String, address : String){
        //set the properties for button
        val btnTag = Button(this)
        btnTag.setLayoutParams(
            ActionBar.LayoutParams(
                ActionBar.LayoutParams.WRAP_CONTENT,
                ActionBar.LayoutParams.WRAP_CONTENT
            )
        )
        //set button text
        btnTag.setText(name)
        //add button to the layout
        pairedDisplay.addView(btnTag)
    }

    //START CAMERA CAPTURE ACTIVITY ONCE CONNECTED TO CONTROLLER
    fun capture(){
        val intent = Intent(this, CameraCapture::class.java)
        startActivity(intent)
    }
}