package com.example.pkhacker

import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.os.Binder
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.*
import java.io.IOException
import java.io.InputStream
import java.lang.Error
import java.lang.Runnable
import java.nio.ByteBuffer

const val START_RECORDING : Int = 0
const val STOP_RECORDING : Int = 1
const val SET_KICK_LEFT : Int = 2
const val SET_KICK_RIGHT : Int = 3
const val RECORDING_STATUS_ON = 4
const val RECORDING_STATUS_OFF = 5
const val KICK_STATUS_LEFT = 6
const val KICK_STATUS_RIGHT = 7
const val SAVE_VIDEO_DIALOG = 8
const val DONT_SAVE_VIDEO = 9

class BluetoothService : Service() {

    //CALLBACK OBJECT
    lateinit var bluetoothListener : BluetoothListener

    //CAN CHANGE SO THERE IS A SINGLE OTHER SOCKET
    lateinit var connectedSocket : BluetoothSocket


    private lateinit var bluetoothAdapter: BluetoothAdapter

    private val localBinder = LocalBinder()

    inner class LocalBinder : Binder() {
        // Return this instance of LocalService so clients can call public methods
        fun getService(): BluetoothService = this@BluetoothService
    }

    //LIVE DATA
    //mutablelivedata currently recording
    //mutablelivedata should be recording
    //mutablelivedta kick direction
    //mutablelivedata desired kick direction

    //HANDLER TO NOTIFY UI THREAD
//    mainHandler.post(myRunnable);
    val handlerUI : Handler = Handler(Looper.getMainLooper())
    val runnableUI_sent : Runnable = object : Runnable{
        override fun run() {
            Toast.makeText(applicationContext, "SENT MESSAGE", Toast.LENGTH_SHORT).show()
        }

    }
    val runnableUI_recieved : Runnable = object : Runnable{
        override fun run() {
            Toast.makeText(applicationContext, "RECIEVED MESSAGE", Toast.LENGTH_SHORT).show()
        }
    }



    //MANAGING BLUETOOTH CONNECTION FUNCTIONS
    fun listen(sender : BluetoothSocket = connectedSocket) = GlobalScope.launch(
            newSingleThreadContext("LISTEN_THREAD")) {
        val inputStream : InputStream = sender.inputStream
        Log.i("BLUETOOTH SERVICE", "STARTING LISTENING LOOP FOR CLIENT============================================")
        while (isActive){
            //GRAB COMMAND FROM CLIENT
            try{
                var instruction : Int = inputStream.read()
//                handlerUI.post(runnableUI_recieved)
                //create toast message with recieved data
//                val runnable : Runnable = Runnable { Toast.makeText(applicationContext, instruction.toString(), Toast.LENGTH_SHORT).show() }
//                handlerUI.post(runnable)
                executeInstruction(instruction)
            }catch(err : IOException){
                Log.i("BLUETOOTH SERVICE", "COULD NOT READ BYTES FROM CLIENT??????????????????????????????????????????????")
                break
            }
        }
    }
    //EXECUTE THE INSTRUCTION
    fun executeInstruction(instruction : Int){
        when(instruction) {
            START_RECORDING -> {
                bluetoothListener.startRecording()
            }
            STOP_RECORDING -> {
                bluetoothListener.stopRecording()
            }
            SET_KICK_LEFT -> {
                bluetoothListener.setKickDirection(KICK_LEFT)
            }
            SET_KICK_RIGHT -> {
                bluetoothListener.setKickDirection(KICK_RIGHT)
            }
            RECORDING_STATUS_ON -> {
                bluetoothListener.currentRecordingStatus(IS_RECORDING)
            }
            RECORDING_STATUS_OFF -> {
                bluetoothListener.currentRecordingStatus(NOT_RECORDING)
            }
            KICK_STATUS_LEFT -> {
                bluetoothListener.currentKickDirection(KICK_LEFT)
            }
            KICK_STATUS_RIGHT -> {
                bluetoothListener.currentKickDirection(KICK_RIGHT)
            }
            SAVE_VIDEO_DIALOG -> {
                bluetoothListener.promptSaveVideoDialog()
            }
            DONT_SAVE_VIDEO -> {
                bluetoothListener.dontSaveVideo()
            }
        }
    }
    //CONVERT BETEWEEN BYTES AND INTS
    fun bytesToInt(bytes: ByteArray): Int {
        var result = 0
        for (i in bytes.indices) {
            result = result or (bytes[i].toInt() shl 8 * i)
        }
        return result
    }
    fun intToBytes(n : Int) : ByteArray{
        return  ByteBuffer.allocate(4).putInt(n).array()
    }
    //WRITING BLUETOOTH FUNCTION
    fun write(reciever : BluetoothSocket = connectedSocket, instruction : Int = 1) = GlobalScope.launch(Dispatchers.IO) {
        try{
            reciever.outputStream.write(instruction)
//            handlerUI.post(runnableUI_sent)
        }catch(err : Error){
            Log.i("Bruh", "FAILED TO SEND DATA")
        }
    }


    //STARTING AND BINDING TO SERVICE FUNCTIONS
    override fun onCreate() {
        super.onCreate()
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        Log.d("BluetoothService", "Created BLUETOOTH SERVICE==========================================")
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i("BLUETOOTH SERVICE", "ON START COMMAND =====================================================")
        return super.onStartCommand(intent, flags, startId)
    }
    override fun onBind(intent: Intent): IBinder {
        Log.i("BLUETOOTH SERVICE", "BINDED TO SERVICE=============================================")
        return localBinder
    }

    //CREATE BLUETOOTH CONNECTION FUNCTIONS
    fun cancelDiscovery(){
        bluetoothAdapter.cancelDiscovery()
    }
    fun queryPairedDevices() : Set<BluetoothDevice> {
        return bluetoothAdapter.bondedDevices
    }
    fun getServerSocket() : BluetoothServerSocket {
        return bluetoothAdapter.listenUsingRfcommWithServiceRecord("com.example.pkhacker", BLUETOOTH_UUID)
    }


    //CALLBACK LISTENER INTERFACE

    public interface BluetoothListener{
        fun startRecording()
        fun stopRecording()
        fun setKickDirection(direction : Int)
        fun currentRecordingStatus(status : Int)
        fun currentKickDirection(status : Int)
        fun promptSaveVideoDialog()
        fun dontSaveVideo()
    }


    fun destroy(){
        Log.i("BLUETOOTHSERVCE", "DESTROYING SERVICE===================================")
        stopSelf()
        Log.i("BLUETOOTH SERVICE", "SERVICE DESTROYED==================================================")
    }
}