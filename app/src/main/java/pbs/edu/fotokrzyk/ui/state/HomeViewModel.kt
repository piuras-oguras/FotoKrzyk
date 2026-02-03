package pbs.edu.fotokrzyk.ui.state

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pbs.edu.fotokrzyk.data.AppDatabase
import pbs.edu.fotokrzyk.data.Measurement
import pbs.edu.fotokrzyk.data.Repository
import pbs.edu.fotokrzyk.util.Permissions
import java.io.File
import kotlin.math.log10
import kotlin.math.round

class HomeViewModel(app: Application) : AndroidViewModel(app) {

    private val TAG = "HomeViewModel"

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
        val app = getApplication<Application>()

        if (!Permissions.has(app, Manifest.permission.RECORD_AUDIO)) {
            _state.update { it.copy(message = "Brak uprawnienia do mikrofonu.") }
            return
        }
        if (!Permissions.has(app, Manifest.permission.ACCESS_FINE_LOCATION)) {
            _state.update { it.copy(message = "Brak uprawnienia do lokalizacji (FINE).") }
            return
        }

        _state.update { it.copy(isSaving = true, message = "Pomiar hałasu...") }

        val tempFile = File(app.cacheDir, "temp_audio_${System.currentTimeMillis()}.m4a")

        val mr = try {
            (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(app) else MediaRecorder()).apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)

                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(44100)
                setAudioEncodingBitRate(128_000)

                setOutputFile(tempFile.absolutePath)
                prepare()
                start()
            }
        } catch (e: Exception) {
            Log.e(TAG, "MediaRecorder start failed", e)
            recorder?.release()
            recorder = null
            if (tempFile.exists()) tempFile.delete()
            _state.update { it.copy(isSaving = false, message = "Błąd startu mikrofonu: ${e.message}") }
            return
        }

        recorder = mr

        viewModelScope.launch {
            var maxAmp = 0

            repeat(20) {
                delay(100)
                val amp = try {
                    recorder?.maxAmplitude ?: 0
                } catch (e: Exception) {
                    Log.e(TAG, "maxAmplitude failed", e)
                    0
                }
                if (amp > maxAmp) maxAmp = amp
            }

            try {
                recorder?.stop()
            } catch (e: Exception) {
                Log.e(TAG, "MediaRecorder stop failed", e)
            } finally {
                try { recorder?.release() } catch (_: Exception) {}
                recorder = null
                if (tempFile.exists()) tempFile.delete()
            }

            Log.d(TAG, "Measured maxAmp=$maxAmp")

            val safeAmp = maxAmp.coerceAtLeast(1)
            val approxDbRaw = 20 * log10(safeAmp.toDouble())
            val approxDb = round(approxDbRaw * 100) / 100

            _state.update { it.copy(lastDb = approxDb, message = "Odczyt lokalizacji...") }

            locationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                CancellationTokenSource().token
            ).addOnSuccessListener { location ->
                if (location != null) {
                    _state.update { it.copy(lastLat = location.latitude, lastLng = location.longitude) }
                } else {
                    _state.update { it.copy(message = "Nie udało się pobrać lokalizacji (null).") }
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
                    _state.update { it.copy(isSaving = false, message = "Zapisano!") }
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        try { recorder?.release() } catch (_: Exception) {}
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

