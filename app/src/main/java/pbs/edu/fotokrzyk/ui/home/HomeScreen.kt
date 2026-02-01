package pbs.edu.fotokrzyk.ui.home

import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import pbs.edu.fotokrzyk.ui.state.HomeViewModel
import pbs.edu.fotokrzyk.util.Permissions
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onGoHistory: () -> Unit,
    vm: HomeViewModel = viewModel()
) {
    val ctx = LocalContext.current
    val state by vm.state.collectAsState()

    var pendingPhotoUri by remember { mutableStateOf<Uri?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* wynik sprawdzamy “na bieżąco” przy klikach */ }

    val photoLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { ok ->
        if (ok) vm.setPhotoUri(pendingPhotoUri?.toString())
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("FotoHałas") }) }
    ) { pad ->
        Column(
            modifier = Modifier
                .padding(pad)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Zrób pomiar: mikrofon + GPS + zdjęcie, a potem zapis do historii.")

            Button(
                onClick = {
                    if (!hasAll(ctx)) {
                        permissionLauncher.launch(Permissions.required)
                        return@Button
                    }
                    vm.measureAndSave()
                },
                enabled = !state.isSaving,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (state.isSaving) "Zapisywanie..." else "Zapisz pomiar")
            }

            OutlinedButton(
                onClick = {
                    if (!hasAll(ctx)) {
                        permissionLauncher.launch(Permissions.required)
                        return@OutlinedButton
                    }
                    val uri = createTempImageUri(ctx)
                    pendingPhotoUri = uri
                    photoLauncher.launch(uri)
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Zrób zdjęcie miejsca") }

            OutlinedButton(
                onClick = onGoHistory,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Przejdź do historii") }

            state.message?.let {
                Text(it, color = MaterialTheme.colorScheme.primary)
            }

            Divider()

            Text("Ostatni wynik (zapisany):")
            Text("dB (orientacyjnie): ${state.lastDb ?: "-"}")
            Text("GPS: ${state.lastLat ?: "-"}, ${state.lastLng ?: "-"}")
            Text("Zdjęcie Uri: ${state.lastPhotoUri ?: "-"}")
        }
    }
}

private fun hasAll(ctx: Context): Boolean =
    Permissions.required.all {
        ContextCompat.checkSelfPermission(ctx, it) == PackageManager.PERMISSION_GRANTED
    }

private fun createTempImageUri(ctx: Context): Uri {
    val dir = ctx.cacheDir
    val file = File(dir, "photo_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", file)
}
