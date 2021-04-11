package com.example.pkhacker.controller

import android.app.AlertDialog
import android.content.DialogInterface
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.example.pkhacker.*

class CameraControl : AppCompatActivity() {

    lateinit var viewModel : MainView

    //VIEWS
    lateinit var sendButton : Button
    lateinit var kickDirectionView : TextView
    lateinit var recordingStatusView : TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera_control)

        viewModel = ViewModelProvider(this).get(MainView()::class.java).also { viewModel ->
            viewModel.initViewModel(applicationContext, fromController = true)
        }
        initializeviews()
        initializeObservers()
    }
    fun initializeviews(){
        sendButton = findViewById<Button>(R.id.sendZero).also{
            it.visibility = View.INVISIBLE
        }
        kickDirectionView = findViewById<Button>(R.id.kickDirection)
        recordingStatusView = findViewById<Button>(R.id.recordingStatus)
    }
    fun initializeObservers(){
        viewModel.bluetoothServiceConnected.observe(this, Observer<Boolean>{ connected ->
            if(connected){
                Log.i("bruh", "CONNECTED TO BLUETOOTH SERVICE")
                sendButton.visibility = View.VISIBLE
            }
        })
        viewModel.monitorKickDirection.observe(this, Observer<Int>{ status ->
            if(status == KICK_LEFT){
                    kickDirectionView.text = "LEFT"
            }else if(status == KICK_RIGHT){
                kickDirectionView.text = "RIGHT"
            }
        })
        viewModel.monitorRecordingStatus.observe(this, Observer<Int>{ status ->
            if(status == IS_RECORDING){
                recordingStatusView.text = "RECORDING"
            }else if(status == NOT_RECORDING){
                recordingStatusView.text = "NOT RECORDING"
            }
        })
        viewModel.promptDialog.observe(this, Observer<Boolean>{ trigger ->
            if(trigger){
                viewModel.promptDialog.postValue(false)
                saveVideoDialog()
            }
        })
    }
    fun sendMessageZero(view : View)
    {
        //start recoring
        viewModel.sendMessage(START_RECORDING)
    }
    fun sendMessageOne(view : View){
        //stop recording
        viewModel.sendMessage(STOP_RECORDING)
    }
    fun kickLeft(view : View){
        viewModel.sendMessage(SET_KICK_LEFT)
    }
    fun kickRight(view : View){
        viewModel.sendMessage(SET_KICK_RIGHT)
    }

    fun saveVideoDialog(){
        // build alert dialog
        val dialogBuilder = AlertDialog.Builder(this)

        // set message of alert dialog
        dialogBuilder.setMessage("Save Recording?")
            // if the dialog is cancelable
            .setCancelable(false)
            // positive button text and action
            .setPositiveButton("Save", DialogInterface.OnClickListener {
                    dialog, id -> return@OnClickListener Unit
            })
            // negative button text and action
            .setNegativeButton("Delete", DialogInterface.OnClickListener {
                    dialog, id ->
                //tell capturer to delete most revent vid
                viewModel?.sendMessage(instruction = DONT_SAVE_VIDEO)
            })

        // create dialog box
        val alert = dialogBuilder.create()
        // set title for alert dialog box
        alert.setTitle("SaveVideoDialog")
        // show alert dialog
        alert.show()
    }




}