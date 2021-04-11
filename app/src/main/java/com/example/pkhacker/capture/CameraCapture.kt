package com.example.pkhacker.capture

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.core.VideoCapture
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.pkhacker.MainView
import com.example.pkhacker.R
import com.example.pkhacker.controller.TestActivity

class CameraCapture : AppCompatActivity() {

    lateinit var viewModel : MainView

    //VIEWS
    lateinit var listenButton : Button
    private lateinit var camPreview : PreviewView

    //CAMERA USE CASES
    private lateinit var videoCapture: VideoCapture

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera_capture)

        viewModel = ViewModelProvider(this).get(MainView()::class.java).also{ viewModel ->
            viewModel.initViewModel(applicationContext)
        }

        initializeviews()
        initializeObservers()
        //get camera permissions
        if (allPermissionsGranted()) {
            activateCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, CameraCapture.REQUIRED_PERMISSIONS, CameraCapture.REQUEST_CODE_PERMISSIONS
            )
        }

    }
    //SEE IF PERMISSIONS WERE GRANTED
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults:
        IntArray) {
        if (requestCode == CameraCapture.REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                activateCamera()
            } else {
                Toast.makeText(this,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    fun initializeObservers(){
        viewModel.bluetoothServiceConnected.observe(this, Observer<Boolean>{ connected ->
            if(connected){
                Log.i("bruh", "CONNECTED TO BLUETOOTH SERVICE")
                listenButton.visibility = View.VISIBLE
            }
        })
    }
    fun initializeviews(){
        listenButton = findViewById<Button>(R.id.listenButton).also{
            it.visibility = View.INVISIBLE
        }
        camPreview = findViewById(R.id.previewView)
    }
    fun startListening(view : View)
    {
        viewModel.listenToController()
    }

    //CHECK CAMERA PERMISSIONS
    private fun allPermissionsGranted() = CameraCapture.REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("RestrictedApi")
    private fun activateCamera(){
        //request cam provider
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        //wait until cam provider is available
        cameraProviderFuture.addListener(Runnable {
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(camPreview.surfaceProvider)
                }

            val videoCaptureConfig = VideoCapture.DEFAULT_CONFIG.config // default config for video capture
            // The Configuration of video capture
            videoCapture = VideoCapture.Builder
                .fromConfig(videoCaptureConfig)
                .build()

            //give view model the videocapture
            viewModel.videoCapture = videoCapture

            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, videoCapture)
//                cameraProvider.bindToLifecycle(
//                    this, cameraSelector, preview)

            } catch(exc: Exception) {
                Log.e("TEST CAM", "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }


    companion object {
        private const val TAG = "CameraXBasic"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        const val REQUEST_CODE_PERMISSIONS = 10
        val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
    }




}