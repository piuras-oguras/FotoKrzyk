package pbs.edu.fotokrzyk.ui.history

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import pbs.edu.fotokrzyk.ui.components.MeasurementCard
import pbs.edu.fotokrzyk.ui.state.HistoryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onBack: () -> Unit,
    vm: HistoryViewModel = viewModel()
) {
    val items by vm.items.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Historia pomiarów") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("Wróć") }
                }
            )
        }
    ) { pad ->
        Column(
            modifier = Modifier
                .padding(pad)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { vm.shareCsvBySms() }) { Text("Eksport SMS (CSV)") }
                OutlinedButton(onClick = { vm.clearAll() }) { Text("Reset") }
            }

            Text("Liczba rekordów: ${items.size}")

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(items) { m ->
                    MeasurementCard(m)
                }
            }
        }
    }
}
