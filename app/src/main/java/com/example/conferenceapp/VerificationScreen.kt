package com.example.conferenceapp

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.conferenceapp.VerificationViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerificationScreen(
    vm: VerificationViewModel,
    onGoRegister: () -> Unit
) {
    val s by vm.state.collectAsState()
    val scheme = MaterialTheme.colorScheme

    val panelColor: Color = when {
        s.notFound -> Color(0xFFB00020) // RED
        s.result == null -> scheme.surfaceVariant
        else -> when (s.result!!.registrationType) {
            1 -> Color(0xFF0F7B0F) // GREEN
            2 -> Color(0xFF0B57D0) // BLUE
            3 -> Color(0xFFFF8C00) // ORANGE
            else -> scheme.surfaceVariant
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Participant Verification") },
                navigationIcon = {
                    IconButton(onClick = onGoRegister) {
                        Text("←")
                    }
                }
            )
        }
    ) { pad ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = s.searchIdText,
                onValueChange = { vm.setSearchIdText(it.filter { ch -> ch.isDigit() }) },
                label = { Text("User ID") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = { vm.verify() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = scheme.primary,
                    contentColor = scheme.onPrimary
                )
            ) { Text("Verify") }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(panelColor)
                    .padding(16.dp)
            ) {
                if (s.result == null) {
                    Column(horizontalAlignment = Alignment.Start) {
                        Text(
                            text = s.message ?: "Enter an ID and press Verify.",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "If User Not Found Background will be red.",
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                } else {
                    val p = s.result!!
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        AsyncImage(
                            model = p.photoUri?.let { Uri.parse(it) },
                            contentDescription = "Profile",
                            modifier = Modifier
                                .size(96.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White.copy(alpha = 0.25f))
                        )
                        Column {
                            Text("User Found ✅", color = Color.White, style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(6.dp))
                            Text("Name: ${p.fullName}", color = Color.White)
                            Text("Title: ${p.title}", color = Color.White)
                            Text("Type: ${p.registrationType}", color = Color.White)
                        }
                    }
                }
            }

            ElevatedCard {
                Column(Modifier.padding(12.dp)) {
                    Text("Color Rules", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    Text("Not Found → RED")
                    Text("Type 1 (Full) → GREEN")
                    Text("Type 2 (Student) → BLUE")
                    Text("Type 3 (None) → ORANGE")
                }
            }
        }
    }
}