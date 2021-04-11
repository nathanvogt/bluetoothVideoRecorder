package com.example.pkhacker.controller

import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.example.pkhacker.R
import com.google.common.util.concurrent.ListenableFuture
import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.ContentValues
import android.content.DialogInterface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import androidx.annotation.RequiresApi
import androidx.camera.core.*
import androidx.camera.core.impl.VideoCaptureConfig
import androidx.core.app.ActivityCompat
import com.example.pkhacker.capture.CameraCapture
import java.io.File

@SuppressLint("RestrictedApi")
class TestActivity : AppCompatActivity() {


    private lateinit var camPreview : PreviewView

    private lateinit var videoCapture: VideoCapture


    override fun onCreate(savedInstanceState: Bundle?) {


        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test)

        initializeViews()

        //get camera permissions
        if (allPermissionsGranted()) {
            activateCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, CameraCapture.REQUIRED_PERMISSIONS, CameraCapture.REQUEST_CODE_PERMISSIONS
            )
        }
    }
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

    private fun initializeViews(){
        camPreview = findViewById(R.id.previewView)
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



    // The Folder location where all the files will be stored
    protected val outputDirectory: String by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            "${Environment.DIRECTORY_DCIM}/CameraXDemo/"
        } else {
            "${applicationContext.getExternalFilesDir(Environment.DIRECTORY_DCIM)?.path}/CameraXDemo/"
        }
    }
    //CAMERA CONTROLS
    private fun allPermissionsGranted() = CameraCapture.REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it) == PackageManager.PERMISSION_GRANTED
    }
    @RequiresApi(Build.VERSION_CODES.P)
    fun testStartRecording(view : View){
        Toast.makeText(applicationContext, "STARTED RECORDING", Toast.LENGTH_SHORT).show()
        //make file location to save to
        File(outputDirectory).mkdirs()
        val file = File("$outputDirectory/${System.currentTimeMillis()}_LEFT.mp4")
        //create output options from file location
        val outputOptions = VideoCapture.OutputFileOptions.Builder(file).build()
        //start recording
        videoCapture.startRecording(outputOptions, applicationContext.mainExecutor, object : VideoCapture.OnVideoSavedCallback { // the callback after recording a video
            override fun onVideoSaved(outputFileResults: VideoCapture.OutputFileResults) {
                //This function is called once video is saved
                outputFileResults.savedUri?.let { saveVideoDialog(it) }
            }
            override fun onError(videoCaptureError: Int, message: String, cause: Throwable?) {
                // This function is called if there is an error during recording process
            }
        })
    }
    fun testStopRecording(view : View){
        videoCapture.stopRecording()
        Toast.makeText(applicationContext, "STOPPED RECORDING", Toast.LENGTH_SHORT).show()
    }

    fun saveVideoDialog(vidUri : Uri){
        // build alert dialog
        val dialogBuilder = AlertDialog.Builder(this)

        // set message of alert dialog
        dialogBuilder.setMessage("Save Recording?")
            // if the dialog is cancelable
            .setCancelable(false)
            // positive button text and action
            .setPositiveButton("Save", DialogInterface.OnClickListener {
                    dialog, id ->
                return@OnClickListener Unit
            })
            // negative button text and action
            .setNegativeButton("Cancel", DialogInterface.OnClickListener {
                    dialog, id ->
                //delete video file
                val deleteFile = File(vidUri.path)
                deleteFile.delete()
            })

        // create dialog box
        val alert = dialogBuilder.create()
        // set title for alert dialog box
        alert.setTitle("SaveVideoDialog")
        // show alert dialog
        alert.show()
    }


    companion object {
        private const val TAG = "CameraXBasic"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }


}