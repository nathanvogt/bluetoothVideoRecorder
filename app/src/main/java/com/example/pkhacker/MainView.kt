package com.example.pkhacker

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.*
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.camera.core.VideoCapture
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import java.io.File
import java.io.IOException
import java.util.*

val BLUETOOTH_UUID : UUID = UUID.fromString("d1895d63-6e58-4a86-8348-5a803bf69f99")

val KICK_LEFT : Int = 0
val KICK_RIGHT : Int = 1
val NOT_RECORDING : Int = 0
val IS_RECORDING : Int = 1


class MainView : ViewModel() {

    //LIVE DATA
    var connectedToServer : MutableLiveData<Boolean> = MutableLiveData<Boolean>().also {
        it.value = false
    }
    var serverSocket : MutableLiveData<BluetoothSocket> = MutableLiveData()
    var bluetoothServiceConnected : MutableLiveData<Boolean> = MutableLiveData<Boolean>().also{
        it.value = false
    }
    var testNewActivity : MutableLiveData<Boolean> = MutableLiveData<Boolean>().also{
        it.value = false
    }

    var monitorKickDirection : MutableLiveData<Int> = MutableLiveData()
    var monitorRecordingStatus : MutableLiveData<Int> = MutableLiveData()

    //trigger save video dialog
    var promptDialog : MutableLiveData<Boolean> = MutableLiveData<Boolean>(false)

    //OBJECT THAT CONTROLLS CAMRERA
    lateinit var videoCapture : VideoCapture

    //STATUS OF RECORDING
    var recordingStatus : Boolean = false

    //CURRENT SELECTED DIRECTION OF KICK
    var kickDirection : Int = KICK_LEFT

    //MOST RECENTLY SAVED VIDEO URI
    lateinit var mostRecentVideoURI : Uri

    //BLUETOOTH DEVICE TO CONNECT TO
    var destinationDevice : BluetoothDevice? = null

    //Model Classes
    private var bluetoothController : BluetoothService? = null

    //PRIVATE VARIABLES
    private var initializedViewModel = false
    private lateinit var applicationContext : Context

    //VIEWMODEL API
    fun initViewModel(appContext : Context, fromController : Boolean = false){
        if(!initializedViewModel){
            applicationContext = appContext
            val serviceConnection = object : ServiceConnection {
                override fun onServiceDisconnected(name: ComponentName?) {
//                    TODO("Not yet implemented")
                }

                override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                    val binder = service as BluetoothService.LocalBinder
                    bluetoothController = binder.getService()
                    bluetoothServiceConnected.value = true

                    //if being initialized from the controller, start listening for status updates
                    if(fromController){bluetoothController!!.listen()}

                    setupBluetoothListeners()
                    Log.i("VIEWMODEL", "BINDED TO BLUETOOTH SERVICE================================================")
                }

            }
            Intent(applicationContext, BluetoothService::class.java).also { intent ->
                applicationContext.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
            }
            initializedViewModel = true
        }
    }

    //SET OUTPUT DIRECTORY
    // The Folder location where all the files will be stored
    protected val outputDirectory: String by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            "${Environment.DIRECTORY_DCIM}/CameraXDemo/"
        } else {
            "${applicationContext.getExternalFilesDir(Environment.DIRECTORY_DCIM)?.path}/CameraXDemo/"
        }
    }

    //SET UP BLUETOOTH LISTENERS
    private fun setupBluetoothListeners(){
        //set up listeners
        val listeners = object : BluetoothService.BluetoothListener{
            @SuppressLint("RestrictedApi")
            @RequiresApi(Build.VERSION_CODES.P)
            override fun startRecording() {
                if(videoCapture != null && recordingStatus == false){
                    recordingStatus = true
                    //kick direction
                    val direction = if (kickDirection==KICK_LEFT) "_LEFT" else "_RIGHT"
                    //make file location to save to
                    File(outputDirectory).mkdirs()
                    val file = File("$outputDirectory/${System.currentTimeMillis()}${direction}.mp4")
                    //create output options from file location
                    val outputOptions = VideoCapture.OutputFileOptions.Builder(file).build()
                    //start recording
                    videoCapture.startRecording(outputOptions, applicationContext.mainExecutor, object : VideoCapture.OnVideoSavedCallback { // the callback after recording a video
                        override fun onVideoSaved(outputFileResults: VideoCapture.OutputFileResults) {
                            //This function is called once video is saved
                            mostRecentVideoURI = outputFileResults.savedUri!!
                            bluetoothController?.write(instruction = SAVE_VIDEO_DIALOG)
                        }
                        override fun onError(videoCaptureError: Int, message: String, cause: Throwable?) {
                            // This function is called if there is an error during recording process
                        }
                    })
                }
            }
            @SuppressLint("RestrictedApi")
            override fun stopRecording() {
                if(videoCapture != null && recordingStatus == true){
                    recordingStatus = false
                    videoCapture.stopRecording()
                }
            }
            override fun setKickDirection(direction: Int) {
                if(recordingStatus == false){
                    kickDirection = direction
                }
            }
            override fun currentRecordingStatus(status : Int){
                if(status!=monitorRecordingStatus.value){
                    monitorRecordingStatus.postValue(status)
                }
            }
            override fun currentKickDirection(status : Int){
                if(status != monitorKickDirection.value){
                    monitorKickDirection.postValue(status)
                }
            }
            //*CONTROLLER* ask whether to save video on capturer
            override fun promptSaveVideoDialog() {
                promptDialog.postValue(true)
            }
            //*CAPTURER* delete most recent video if told to from controller
            override fun dontSaveVideo() {
                if(mostRecentVideoURI != null){
                    val file = File(mostRecentVideoURI.path)
                    if(file.exists()){
                        file.delete()
                    }
                }
            }
        }
        //pass listeners to bluetooth service
        bluetoothController!!.bluetoothListener = listeners
    }


    //Bluetooth API
    fun pairedDevices() : Set<BluetoothDevice>? {
        if(bluetoothController != null){return bluetoothController!!.queryPairedDevices()}
        return null
    }
    fun listenForConnections() {
        if(bluetoothController == null){return Unit}
        Log.i("ViewModel", "LISTENING FOR CONNECTIONS FOR 12 SECONDS=======================================")
        viewModelScope.launch(Dispatchers.IO) {
            val serverListener = bluetoothController!!.getServerSocket()
            lateinit var socket: BluetoothSocket
            try {
                socket = serverListener.accept(12000)
                serverListener.close()
                withContext(Dispatchers.Main) {

                    bluetoothController!!.connectedSocket = socket
                    serverSocket.value = socket
                    Log.i("===========ViewModel", "Succesfully CONNECTED TO DEVICE!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!")
                }
            } catch (err: IOException) {
                serverListener.close()
//                withContext(Dispatchers.Main){
//                    testNewActivity.value = true
//                }
                Log.i("ViewModel", "Couldnt connect to remote device???????????????????????????????????????????????????????????????????????????????????????")
            }
        }
    }

    fun requestConnection() = viewModelScope.launch(Dispatchers.IO) {
        if(destinationDevice != null){
            bluetoothController!!.cancelDiscovery()
            val clientSocket = destinationDevice!!.createRfcommSocketToServiceRecord(
                BLUETOOTH_UUID)
            try {
                val result = clientSocket.connect()
                bluetoothController!!.connectedSocket = clientSocket
                withContext(Dispatchers.Main){
                    connectedToServer.value = true
                }

                Log.i("===========ViewModel", "Succesfully CONNECTED TO DEVICE!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!")
                //TODO("do stuff with connected socket")
            } catch (err : Error){
                Log.i("ViewModel", "Couldnt connect to remote device???????????????????????????????????????????????????????????????????????????????????????")
            }
        }
    }

    fun listenToController(){
        bluetoothController?.listen()
        updateControllerStatus()
    }
    //SENDS MESSAGES TO CONTROLLER TO UPDATE STATE OF RECORDING/KICK DIRECTION
    fun updateControllerStatus() = GlobalScope.launch(newSingleThreadContext("UPDATE_CONTROLLER")) {
        delay(500L)
        while (isActive) {
            if (recordingStatus == true) {
                bluetoothController?.write(instruction = RECORDING_STATUS_ON)
            } else if (recordingStatus == false) {
                bluetoothController?.write(instruction = RECORDING_STATUS_OFF)
            }
            if (kickDirection == KICK_LEFT) {
                bluetoothController?.write(instruction = KICK_STATUS_LEFT)
            } else if (kickDirection == KICK_RIGHT) {
                bluetoothController?.write(instruction = KICK_STATUS_RIGHT)
            }
            delay(100L)
        }
    }

    //TEST
    fun sendMessage(instruction : Int = 69){
        bluetoothController!!.write(instruction = instruction)
    }

    //DIALOG TO SAVE OR DELETE VIDEO RECORDING






    override fun onCleared() {
        super.onCleared()
        //TODO("Stop Service")
        if(bluetoothController != null){
            bluetoothController!!.destroy()
        }

    }
}


