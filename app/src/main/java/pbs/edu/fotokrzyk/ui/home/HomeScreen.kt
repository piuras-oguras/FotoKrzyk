package pbs.edu.fotokrzyk.ui.home

import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import pbs.edu.fotokrzyk.ui.state.HomeViewModel
import pbs.edu.fotokrzyk.util.Permissions
import java.io.File
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import pbs.edu.fotokrzyk.R
private enum class PendingAction {
    NONE,
    MEASURE_AND_SAVE,
    TAKE_PHOTO
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onGoHistory: () -> Unit,
    vm: HomeViewModel = viewModel()
) {
    val ctx = LocalContext.current
    val state by vm.state.collectAsState()

    var pendingAction by remember { mutableStateOf(PendingAction.NONE) }
    var pendingPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var localMessage by remember { mutableStateOf<String?>(null) }

    val photoLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { ok ->
        if (ok) {
            vm.setPhotoUri(pendingPhotoUri?.toString())
            localMessage = null
        } else {
            localMessage = "Nie udało się wykonać zdjęcia (anulowano lub błąd aparatu)."
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val allGranted = Permissions.required.all { perm -> result[perm] == true }

        if (!allGranted) {
            localMessage = "Brak wymaganych uprawnień. Bez nich nie da się wykonać pomiaru."
            pendingAction = PendingAction.NONE
            return@rememberLauncherForActivityResult
        }

        localMessage = null

        when (pendingAction) {
            PendingAction.MEASURE_AND_SAVE -> {
                pendingAction = PendingAction.NONE
                vm.measureAndSave()
            }

            PendingAction.TAKE_PHOTO -> {
                pendingAction = PendingAction.NONE
                val uri = createTempImageUri(ctx)
                pendingPhotoUri = uri
                photoLauncher.launch(uri)
            }

            PendingAction.NONE -> Unit
        }
    }

    fun ensurePermissionsThen(action: PendingAction) {
        if (hasAll(ctx)) {
            localMessage = null
            when (action) {
                PendingAction.MEASURE_AND_SAVE -> vm.measureAndSave()
                PendingAction.TAKE_PHOTO -> {
                    val uri = createTempImageUri(ctx)
                    pendingPhotoUri = uri
                    photoLauncher.launch(uri)
                }
                PendingAction.NONE -> Unit
            }
            return
        }
        pendingAction = action
        permissionLauncher.launch(Permissions.required)
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("FotoKrzyk") }) }
    ) { pad ->
        Column(
            modifier = Modifier
                .padding(pad)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Zarejestruj miejsce gdzie jest hałas. ")
            Image(
                painter = painterResource(id = R.drawable.kotek),
                contentDescription = "Kotek – hałas",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .height(120.dp),
                contentScale = ContentScale.Fit
            )
            Button(
                onClick = { ensurePermissionsThen(PendingAction.MEASURE_AND_SAVE) },
                enabled = !state.isSaving,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (state.isSaving) "Zapisywanie..." else "Zapisz pomiar")
            }

            OutlinedButton(
                onClick = { ensurePermissionsThen(PendingAction.TAKE_PHOTO) },
                enabled = !state.isSaving,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Zrób zdjęcie miejsca") }

            OutlinedButton(
                onClick = onGoHistory,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Przejdź do historii") }

            (localMessage ?: state.message)?.let {
                Text(it, color = MaterialTheme.colorScheme.primary)
            }
            Divider()
            Text("dB (orientacyjnie): ${state.lastDb ?: "-"}")
            Text("GPS: ${state.lastLat ?: "-"}, ${state.lastLng ?: "-"}")
            Text("Zdjęcie Uri: ${state.lastPhotoUri ?: "-"}")
        }
    }
}

private fun hasAll(ctx: Context): Boolean =
    Permissions.required.all { perm ->
        ContextCompat.checkSelfPermission(ctx, perm) == PackageManager.PERMISSION_GRANTED
    }

private fun createTempImageUri(ctx: Context): Uri {
    val dir = ctx.cacheDir
    val file = File(dir, "photo_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", file)
}
