package com.example.conferenceapp

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.Observer
import com.example.conferenceapp.ui.theme.ConferenceTheme
import java.io.File

/**
 * In-app camera capture using CameraX.
 * This avoids relying on vendor camera apps which may crash on some devices/emulators.
 *
 * Started via Intent:
 *   putExtra(EXTRA_FILE_PATH, <absolute file path>)
 *
 * Result:
 *   RESULT_OK + EXTRA_RESULT_URI (FileProvider uri as string)
 */
class CameraCaptureActivity : ComponentActivity() {

    companion object {
        const val EXTRA_FILE_PATH = "extra_file_path"
        const val EXTRA_RESULT_URI = "extra_result_uri"
    }

    private var imageCapture: ImageCapture? = null
    private lateinit var outputFile: File
    private lateinit var outputUri: android.net.Uri

    // ---- Stream watchdog (black-screen / unplug handling) ----
    private val handler = Handler(Looper.getMainLooper())
    private var watcherAttached = false
    private var streamingStarted = false
    private var idleRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Permission guard (should already be granted before starting this activity)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            setResult(Activity.RESULT_CANCELED)
            finish()
            return
        }

        val filePath = intent.getStringExtra(EXTRA_FILE_PATH)
        if (filePath.isNullOrBlank()) {
            setResult(Activity.RESULT_CANCELED)
            finish()
            return
        }

        outputFile = File(filePath)
        outputFile.parentFile?.mkdirs()

        outputUri = FileProvider.getUriForFile(
            this,
            "${packageName}.fileprovider",
            outputFile
        )

        setContent {
            ConferenceTheme {
                CameraCaptureScreen(
                    onBindPreview = { previewView ->
                        // attach once
                        if (!watcherAttached) {
                            watcherAttached = true
                            attachStreamWatchdog(previewView)
                        }
                        bindCamera(previewView)
                    },
                    onCapture = { capturePhoto() },
                    onCancel = {
                        setResult(Activity.RESULT_CANCELED)
                        finish()
                    }
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // prevent leaks / callbacks firing after finish
        idleRunnable?.let { handler.removeCallbacks(it) }
        handler.removeCallbacksAndMessages(null)
    }

    private fun attachStreamWatchdog(previewView: PreviewView) {
        // 1) If preview never starts streaming quickly, bail out with a friendly message.
        handler.postDelayed({
            if (!streamingStarted) {
                Toast.makeText(
                    this,
                    "Camera is not available (or disconnected). Returning…",
                    Toast.LENGTH_LONG
                ).show()
                setResult(Activity.RESULT_CANCELED)
                finish()
            }
        }, 5000)

        // 2) If it was streaming and later goes idle for a while -> treat as disconnect, bail out.
        previewView.previewStreamState.observe(
            this,
            Observer { state ->
                if (state == PreviewView.StreamState.STREAMING) {
                    streamingStarted = true
                    idleRunnable?.let { handler.removeCallbacks(it) }
                    idleRunnable = null
                } else {
                    // if we already had a good stream before, and now it's not streaming -> likely unplug / failure
                    if (streamingStarted) {
                        val r = Runnable {
                            val current = previewView.previewStreamState.value
                            if (current != PreviewView.StreamState.STREAMING) {
                                Toast.makeText(
                                    this,
                                    "Camera disconnected. Returning…",
                                    Toast.LENGTH_LONG
                                ).show()
                                setResult(Activity.RESULT_CANCELED)
                                finish()
                            }
                        }
                        idleRunnable?.let { handler.removeCallbacks(it) }
                        idleRunnable = r
                        handler.postDelayed(r, 1200)
                    }
                }
            }
        )
    }

    private fun bindCamera(previewView: PreviewView) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val provider = cameraProviderFuture.get()
            provider.unbindAll()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            val capture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            imageCapture = capture

            // Prefer back camera, fallback to front
            val bound = tryBind(provider, CameraSelector.DEFAULT_BACK_CAMERA, preview, capture) ||
                    tryBind(provider, CameraSelector.DEFAULT_FRONT_CAMERA, preview, capture)

            if (!bound) {
                Toast.makeText(this, "No camera found on this device.", Toast.LENGTH_LONG).show()
                setResult(Activity.RESULT_CANCELED)
                finish()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun tryBind(
        provider: ProcessCameraProvider,
        selector: CameraSelector,
        preview: Preview,
        capture: ImageCapture
    ): Boolean {
        return try {
            provider.bindToLifecycle(this, selector, preview, capture)
            true
        } catch (_: Throwable) {
            false
        }
    }

    private fun capturePhoto() {
        val ic = imageCapture ?: return
        val outputOptions = ImageCapture.OutputFileOptions.Builder(outputFile).build()

        ic.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    setResult(
                        Activity.RESULT_OK,
                        Intent().putExtra(EXTRA_RESULT_URI, outputUri.toString())
                    )
                    finish()
                }

                override fun onError(exception: ImageCaptureException) {
                    Toast.makeText(this@CameraCaptureActivity, "Capture failed.", Toast.LENGTH_SHORT).show()
                    setResult(Activity.RESULT_CANCELED)
                    finish()
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CameraCaptureScreen(
    onBindPreview: (PreviewView) -> Unit,
    onCapture: () -> Unit,
    onCancel: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Take Profile Photo") },
                navigationIcon = {
                    IconButton(onClick = onCancel) { Text("✕") }
                }
            )
        }
    ) { pad ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        PreviewView(ctx).also { pv ->
                            pv.scaleType = PreviewView.ScaleType.FILL_CENTER
                            onBindPreview(pv)
                        }
                    }
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }
                Button(
                    onClick = onCapture,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Capture")
                }
            }
        }
    }
}