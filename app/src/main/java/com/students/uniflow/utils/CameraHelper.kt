package com.students.uniflow.utils

import android.content.Context
import android.net.Uri
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class CameraHelper(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val previewView: PreviewView,
    private val onPhotoCaptured: (Uri) -> Unit
) {

    private var imageCapture: ImageCapture? = null
    private var camera: Camera? = null
    private var isTorchOn = false

    fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            imageCapture = ImageCapture.Builder().build()

            try {
                cameraProvider.unbindAll()
                camera = cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageCapture
                )
            } catch (e: Exception) {
                android.util.Log.e("CameraHelper", "Camera start failed: ${e.message}")
            }

        }, ContextCompat.getMainExecutor(context))
    }

    // Toggle torch on/off — returns new state (true = on)
    fun toggleTorch(): Boolean {
        isTorchOn = !isTorchOn
        camera?.cameraControl?.enableTorch(isTorchOn)
        return isTorchOn
    }

    fun turnOffTorch() {
        isTorchOn = false
        camera?.cameraControl?.enableTorch(false)
    }

    fun takePhoto() {
        val imageCapture = imageCapture ?: return

        val photoFile = File(
            context.cacheDir,
            SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.US).format(Date()) + ".jpg"
        )

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val uri = Uri.fromFile(photoFile)
                    onPhotoCaptured(uri)
                }

                override fun onError(exc: ImageCaptureException) {
                    android.util.Log.e("CameraHelper", "Photo capture failed: ${exc.message}")
                }
            }
        )
    }
}