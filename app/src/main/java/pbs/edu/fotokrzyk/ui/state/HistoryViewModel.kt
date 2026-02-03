package pbs.edu.fotokrzyk.ui.state

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pbs.edu.fotokrzyk.data.AppDatabase
import pbs.edu.fotokrzyk.data.Repository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = Repository(AppDatabase.get(app).dao())

    val items = repo.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun clearAll() {
        viewModelScope.launch { repo.clearAll() }
    }

    fun shareCsvBySms(phoneNumber: String = "") {
        val ctx = getApplication<Application>()
        val list = items.value

        val csv = buildString {
            appendLine("id,timestamp,lat,lng,approxDb,photoUri")
            for (m in list) {
                appendLine("${m.id},${m.timestampMs},${m.lat},${m.lng},${m.approxDb},${m.photoUri}")
            }
        }

        val header = "FotoHałas - eksport pomiarów (${now()})"
        val smsBody = "$header\n\n$csv"

        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("smsto:$phoneNumber")
            putExtra("sms_body", smsBody)
        }

        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ctx.startActivity(intent)
    }

    private fun now(): String =
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
}
