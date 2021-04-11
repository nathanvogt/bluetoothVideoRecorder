package com.example.pkhacker.capture

import android.app.ActionBar
import android.bluetooth.BluetoothDevice
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
import androidx.lifecycle.observe
import com.example.pkhacker.MainView
import com.example.pkhacker.R
import com.example.pkhacker.controller.CameraControl
import java.lang.NullPointerException

class ControlMenu : AppCompatActivity() {

    lateinit var viewModel : MainView

    //UI Views
    lateinit var pairedDisplay : LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_capture_menu)

        initializeViews()

        viewModel = ViewModelProvider(this).get(MainView()::class.java).also{ viewModel ->
            viewModel.initViewModel(applicationContext)
        }
        initializeObservers()

    }

    private fun initializeViews(){
        pairedDisplay = findViewById(R.id.pairedDisplay)
    }
    private fun initializeObservers(){
        viewModel.bluetoothServiceConnected.observe(this, Observer<Boolean>{ connected ->
            if(connected){
                Log.i("bruh", "POPULATING PAIRED DEVICES===============================")
                populatePairedDevices()
            }
        })
        viewModel.connectedToServer.observe(this, Observer<Boolean>{ connected ->
            if(connected){
                control()
            }
        })
    }

    fun populatePairedDevices(){
        try{
            viewModel.pairedDevices()!!.forEach { device ->
//            val deviceName = device.name
//            val deviceAddress = device.address // MAC address
                addButton(device)
            }
        } catch(err : NullPointerException){

        }
    }
    fun addButton(device : BluetoothDevice){
        //set the properties for button
        val btnTag = Button(this)
        btnTag.layoutParams = ActionBar.LayoutParams(
            ActionBar.LayoutParams.WRAP_CONTENT,
            ActionBar.LayoutParams.WRAP_CONTENT
        )
        btnTag.setText(device.name)
        //set button listener
        btnTag.setOnClickListener(View.OnClickListener {
            connectToDevice(device)
        })
        //add button to the layout
        pairedDisplay.addView(btnTag)
    }
    fun connectToDevice(device : BluetoothDevice){
        viewModel.destinationDevice = device
        viewModel.requestConnection()
    }

    //START CONTROLLING CAMERA AFTER CONNECTING TO CAPTURE DEVICE
    fun control(){
        val intent = Intent(this, CameraControl::class.java)
        startActivity(intent)
    }

}