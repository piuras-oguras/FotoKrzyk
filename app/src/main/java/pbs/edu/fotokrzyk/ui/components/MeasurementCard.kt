package pbs.edu.fotokrzyk.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import pbs.edu.fotokrzyk.data.Measurement
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MeasurementCard(m: Measurement) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Pomiar #${m.id}", style = MaterialTheme.typography.titleMedium)
            Text("Czas: ${format(m.timestampMs)}")
            Text("dB (orientacyjnie): ${m.approxDb ?: "-"}")
            Text("GPS: ${m.lat ?: "-"}, ${m.lng ?: "-"}")

            if (!m.photoUri.isNullOrBlank()) {
                Spacer(Modifier.height(6.dp))
                AsyncImage(
                    model = m.photoUri,
                    contentDescription = "Zdjęcie miejsca",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                )
            }
        }
    }
}

private fun format(ms: Long): String =
    SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(ms))
