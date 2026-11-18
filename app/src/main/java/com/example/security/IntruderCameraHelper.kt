package com.example.security

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.io.File
import java.util.concurrent.Executors

class IntruderCameraHelper(
    private val context: Context,
    private val cryptoManager: CryptoManager
) {
    private val cameraExecutor = Executors.newSingleThreadExecutor()

    fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun captureIntruderPhoto(
        lifecycleOwner: LifecycleOwner,
        onCaptured: (encryptedFilePath: String) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!hasCameraPermission()) {
            onError("Camera permission not granted")
            return
        }

        val providerFuture = ProcessCameraProvider.getInstance(context)
        providerFuture.addListener({
            try {
                val cameraProvider = providerFuture.get()
                val selector = when {
                    cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) ->
                        CameraSelector.DEFAULT_FRONT_CAMERA
                    cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) ->
                        CameraSelector.DEFAULT_BACK_CAMERA
                    else -> {
                        onError("No camera available on device")
                        return@addListener
                    }
                }

                val imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(lifecycleOwner, selector, imageCapture)

                val intrudersDir = File(context.filesDir, "intruders").apply { mkdirs() }
                val tempPlain = File(context.cacheDir, "intruder_tmp_${System.currentTimeMillis()}.jpg")
                val outputOptions = ImageCapture.OutputFileOptions.Builder(tempPlain).build()

                imageCapture.takePicture(
                    outputOptions,
                    cameraExecutor,
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                            try {
                                val encryptedFile = File(intrudersDir, "intruder_${System.currentTimeMillis()}.enc")
                                tempPlain.inputStream().use { input ->
                                    encryptedFile.outputStream().use { output ->
                                        cryptoManager.encryptStream(input, output)
                                    }
                                }
                                tempPlain.delete()
                                ContextCompat.getMainExecutor(context).execute {
                                    cameraProvider.unbindAll()
                                    onCaptured(encryptedFile.absolutePath)
                                }
                            } catch (e: Exception) {
                                tempPlain.delete()
                                ContextCompat.getMainExecutor(context).execute {
                                    cameraProvider.unbindAll()
                                    onError(e.message ?: "Failed to encrypt intruder photo")
                                }
                            }
                        }

                        override fun onError(exception: ImageCaptureException) {
                            tempPlain.delete()
                            ContextCompat.getMainExecutor(context).execute {
                                cameraProvider.unbindAll()
                                onError(exception.message ?: "Camera capture error")
                            }
                        }
                    }
                )
            } catch (e: Exception) {
                onError(e.message ?: "Camera initialization error")
            }
        }, ContextCompat.getMainExecutor(context))
    }
}
