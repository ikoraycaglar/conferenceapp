package com.example.conferenceapp

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import java.io.File

private const val CONFERENCE_URL = "https://ankarabilim.edu.tr"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistrationScreen(
    vm: RegistrationViewModel,
    onGoVerify: () -> Unit
) {
    val s by vm.state.collectAsState()
    val ctx = LocalContext.current
    val scheme = MaterialTheme.colorScheme

    // Always visible feedback (not hidden off-screen)
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(s.message) {
        val msg = s.message
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            vm.clearMessage()
        }
    }

    fun openConference() {
        val i = Intent(Intent.ACTION_VIEW, Uri.parse(CONFERENCE_URL))
        runCatching { ctx.startActivity(i) }
    }

    // Camera: use in-app CameraX activity to avoid crashing vendor camera apps
    var pendingFilePath by remember { mutableStateOf<String?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
        if (res.resultCode == Activity.RESULT_OK) {
            val uriStr = res.data?.getStringExtra(CameraCaptureActivity.EXTRA_RESULT_URI)
            if (!uriStr.isNullOrBlank()) {
                vm.setPhotoUri(uriStr)
                vm.showMessage("Photo captured ✅")
            } else {
                vm.showMessage("Photo saved, but URI was missing.")
            }
        } else {
            vm.showMessage("Photo capture cancelled.")
        }
    }

    val requestCameraPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            val path = pendingFilePath
            if (path.isNullOrBlank()) {
                vm.showMessage("Unexpected error: missing output file.")
                return@rememberLauncherForActivityResult
            }
            val intent = Intent(ctx, CameraCaptureActivity::class.java)
                .putExtra(CameraCaptureActivity.EXTRA_FILE_PATH, path)
            cameraLauncher.launch(intent)
        } else {
            vm.showMessage("Camera permission denied.")
        }
    }

    fun launchCamera() {
        val cameraOk = runCatching {
            val cm = ctx.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            cm.cameraIdList.isNotEmpty()
        }.getOrDefault(false)

        if (!cameraOk) {
            vm.showMessage("No camera found on this device. You can register without a photo.")
            return
        }

        val dir = File(ctx.filesDir, "profiles").apply { mkdirs() }
        val file = File(dir, "profile_${System.currentTimeMillis()}.jpg")
        pendingFilePath = file.absolutePath

        val hasPermission = ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        if (hasPermission) {
            val intent = Intent(ctx, CameraCaptureActivity::class.java)
                .putExtra(CameraCaptureActivity.EXTRA_FILE_PATH, file.absolutePath)
            cameraLauncher.launch(intent)
        } else {
            requestCameraPermission.launch(Manifest.permission.CAMERA)
        }
    }

    val bg = Brush.verticalGradient(
        listOf(
            scheme.primary.copy(alpha = 0.18f),
            scheme.background
        )
    )

    Scaffold(
        topBar = { TopAppBar(title = { Text("Participant Registration") }) },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { pad ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(bg)
                .padding(pad)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            ElevatedCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Conference Info", style = MaterialTheme.typography.titleMedium)
                        Text("Opens the conference website in your browser.", style = MaterialTheme.typography.bodySmall)
                    }
                    Button(
                        onClick = { openConference() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = scheme.primary,
                            contentColor = Color.White
                        )
                    ) {
                        Text("Open", color = scheme.onPrimary)
                    }
                }
            }

            OutlinedTextField(
                value = s.userIdText,
                onValueChange = { vm.setUserIdText(it.filter { ch -> ch.isDigit() }) },
                label = { Text("User ID (Unique Integer)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = s.fullName,
                onValueChange = { vm.setFullName(it) },
                label = { Text("Full Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Title "Spinner" (robust implementation: works even if ExposedDropdownMenu is missing)
            var titleExpanded by remember { mutableStateOf(false) }
            Box {
                OutlinedTextField(
                    value = s.title,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Title") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { titleExpanded = true }
                )
                DropdownMenu(
                    expanded = titleExpanded,
                    onDismissRequest = { titleExpanded = false }
                ) {
                    listOf("Prof", "Dr.", "Student").forEach { t ->
                        DropdownMenuItem(
                            text = { Text(t) },
                            onClick = {
                                vm.setTitle(t)
                                titleExpanded = false
                            }
                        )
                    }
                }
            }

            ElevatedCard {
                Column(Modifier.padding(12.dp)) {
                    Text("Registration Type", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = s.regType == 1, onClick = { vm.setRegType(1) })
                        Text("1 - Full", modifier = Modifier.clickable { vm.setRegType(1) })
                        Spacer(Modifier.width(12.dp))
                        RadioButton(selected = s.regType == 2, onClick = { vm.setRegType(2) })
                        Text("2 - Student", modifier = Modifier.clickable { vm.setRegType(2) })
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = s.regType == 3, onClick = { vm.setRegType(3) })
                        Text("3 - None", modifier = Modifier.clickable { vm.setRegType(3) })
                    }
                }
            }

            ElevatedCard {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Profile Photo", style = MaterialTheme.typography.titleMedium)

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(scheme.surfaceVariant)
                            .clickable { launchCamera() },
                        contentAlignment = Alignment.Center
                    ) {
                        if (s.photoUri.isNullOrBlank()) {
                            Text("Tap to open camera", style = MaterialTheme.typography.bodyMedium)
                        } else {
                            AsyncImage(
                                model = Uri.parse(s.photoUri),
                                contentDescription = "Profile Photo",
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    Text(
                        "You can register even without a photo.",
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { vm.register() },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = scheme.primary,
                        contentColor = Color.White
                    )
                ) {
                    Text("Register", color = Color.White)
                }

                OutlinedButton(
                    onClick = onGoVerify,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = scheme.primary)
                ) {
                    Text("Verification", color = scheme.primary)
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}