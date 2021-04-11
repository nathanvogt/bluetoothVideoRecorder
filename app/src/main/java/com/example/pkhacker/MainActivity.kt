package com.example.pkhacker

import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.pkhacker.capture.CaptureMenu
import com.example.pkhacker.capture.ControlMenu
import com.example.pkhacker.controller.TestActivity

class MainActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        createBluetoothService()
    }
    private fun createBluetoothService(){
        Intent(this, BluetoothService::class.java).also { intent ->
            startService(intent)
        }
    }


    fun capture(view : View){
        val intent = Intent(this, CaptureMenu::class.java)
        startActivity(intent)
    }
    fun control(view : View){
        val intent = Intent(this, ControlMenu::class.java)
        startActivity(intent)
    }
    fun testActivity(view : View){
        val intent = Intent(this, TestActivity::class.java)
        startActivity(intent)
    }


}