package pbs.edu.fotokrzyk.ui.state

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.media.MediaRecorder
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pbs.edu.fotokrzyk.data.AppDatabase
import pbs.edu.fotokrzyk.data.Measurement
import pbs.edu.fotokrzyk.data.Repository
import pbs.edu.fotokrzyk.util.Permissions
import java.io.File

class HomeViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = Repository(AppDatabase.get(app).dao())
    private var recorder: MediaRecorder? = null
    private val locationClient = LocationServices.getFusedLocationProviderClient(app)

    private val _state = MutableStateFlow(HomeState())
    val state = _state.asStateFlow()

    fun setPhotoUri(uri: String?) {
        _state.update { it.copy(lastPhotoUri = uri) }
    }

    @SuppressLint("MissingPermission")
    fun measureAndSave() {
        if (!Permissions.has(getApplication(), Manifest.permission.RECORD_AUDIO)) return
        if (!Permissions.has(getApplication(), Manifest.permission.ACCESS_COARSE_LOCATION)) return
        if (!Permissions.has(getApplication(), Manifest.permission.ACCESS_FINE_LOCATION)) return

        _state.update { it.copy(isSaving = true, message = "Pomiar hałasu...") }

        val file = File(getApplication<Application>().cacheDir, "temp_audio.3gp")
        recorder = (
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(getApplication())
            else MediaRecorder()
        ).apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            setOutputFile(file.absolutePath)
            prepare()
            start()
        }

        viewModelScope.launch {
            kotlinx.coroutines.delay(2000)
            val maxAmplitude = recorder?.maxAmplitude ?: 0
            recorder?.stop()
            recorder?.release()
            recorder = null
                // zabezpieczenie przed log10(0)
            val safeAmp = maxAmplitude.coerceAtLeast(1) // minimum 1
            val approxDb = 20.0 * kotlin.math.log10(safeAmp.toDouble())
            _state.update { it.copy(lastDb = approxDb, message = "Odczyt lokalizacji...") }

            locationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                CancellationTokenSource().token
            ).addOnSuccessListener { location ->
                _state.update {
                    it.copy(lastLat = location.latitude, lastLng = location.longitude)
                }
            }.addOnCompleteListener {
                viewModelScope.launch {
                    repo.insert(
                        Measurement(
                            timestampMs = System.currentTimeMillis(),
                            lat = _state.value.lastLat,
                            lng = _state.value.lastLng,
                            approxDb = _state.value.lastDb,
                            photoUri = _state.value.lastPhotoUri
                        )
                    )
                    _state.update {
                        it.copy(isSaving = false, message = "Zapisano!")
                    }
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        recorder?.release()
        recorder = null
    }
}

data class HomeState(
    val isSaving: Boolean = false,
    val lastDb: Double? = null,
    val lastLat: Double? = null,
    val lastLng: Double? = null,
    val lastPhotoUri: String? = null,
    val message: String? = null,
)
