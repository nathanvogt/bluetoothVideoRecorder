package com.example.pkhacker

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.util.Log
import androidx.core.app.ActivityCompat.startActivityForResult
import java.util.*

class BluetoothController {
    val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

    fun cancelDiscovery(){
        bluetoothAdapter.cancelDiscovery()
    }
    fun queryPairedDevices() : Set<BluetoothDevice> {
        return bluetoothAdapter.bondedDevices
    }
    fun getServerSocket() : BluetoothServerSocket{
        return bluetoothAdapter.listenUsingRfcommWithServiceRecord("com.example.pkhacker", BLUETOOTH_UUID)
    }




}