package com.example.conferenceapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.conferenceapp.RegistrationViewModel
import com.example.conferenceapp.VerificationViewModel
import com.example.conferenceapp.RegistrationScreen
import com.example.conferenceapp.VerificationScreen
import com.example.conferenceapp.ui.theme.ConferenceTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = application as ConferenceApp
        val repo = app.container.participantRepository

        setContent {
            ConferenceTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    AppNav(repoProvider = { repo })
                }
            }
        }
    }
}

@Composable
private fun AppNav(repoProvider: () -> com.example.conferenceapp.data.ParticipantRepository) {
    val nav = rememberNavController()
    val ctx = LocalContext.current

    fun <T : ViewModel> factory(create: () -> T) =
        object : ViewModelProvider.Factory {
            override fun <VM : ViewModel> create(modelClass: Class<VM>): VM {
                @Suppress("UNCHECKED_CAST")
                return create() as VM
            }
        }

    NavHost(navController = nav, startDestination = "register") {
        composable("register") {
            val vm = androidx.lifecycle.viewmodel.compose.viewModel<RegistrationViewModel>(
                factory = factory { RegistrationViewModel(repoProvider()) }
            )
            RegistrationScreen(
                vm = vm,
                onGoVerify = {
                    nav.navigate("verify") {
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
        composable("verify") {
            val vm = androidx.lifecycle.viewmodel.compose.viewModel<VerificationViewModel>(
                factory = factory { VerificationViewModel(repoProvider()) }
            )
            VerificationScreen(
                vm = vm,
                onGoRegister = { nav.popBackStack("register", inclusive = false) }
            )
        }
    }
}