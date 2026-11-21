package com.verticthub.pyobfuscator.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.verticthub.pyobfuscator.design.components.*
import com.verticthub.pyobfuscator.ui.StudioUiState
import com.verticthub.pyobfuscator.ui.StudioViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(state: StudioUiState, viewModel: StudioViewModel, onOpenResult: () -> Unit) {
    var query by remember { mutableStateOf("") }
    var showClearDialog by remember { mutableStateOf(false) }
    val fmt = remember { SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault()) }

    val filtered = remember(state.history, query) {
        if (query.isBlank()) state.history
        else state.history.filter { it.projectName.contains(query, true) || it.sourceFilename.contains(query, true) || it.configSummary.contains(query, true) }
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        VHSectionHeader(title = "Riwayat Transformasi & Build", subtitle = "${state.history.size} artefak tersimpan di penyimpanan privat aplikasi", trailing = {
            if (state.history.isNotEmpty()) {
                IconButton(onClick = { showClearDialog = true }) { Icon(Icons.Default.Delete, contentDescription = "Hapus Semua") }
            }
        })
        OutlinedTextField(value = query, onValueChange = { query = it }, label = { Text("Cari riwayat proyek...") }, singleLine = true, modifier = Modifier.fillMaxWidth())

        if (filtered.isEmpty()) {
            VHEmptyState(title = "Riwayat Kosong", description = "Hasil transformasi dan build yang berhasil akan otomatis tercatat di sini.")
        } else {
            filtered.forEach { item ->
                val dateStr = fmt.format(Date(item.timestampMillis))
                VHFileCard(
                    filename = "${item.projectName} (${item.sourceFilename})",
                    metadata = "$dateStr • ${item.configSummary}",
                    statusLabel = item.status,
                    statusTone = if (item.status == "SUCCESS") BadgeTone.SUCCESS else BadgeTone.PRIMARY,
                    onClick = { viewModel.openHistoryRecord(item, onOpenResult) },
                )
            }
        }
        Spacer(Modifier.height(24.dp))
    }

    if (showClearDialog) {
        VHConfirmationDialog(title = "Hapus Semua Riwayat?", message = "Seluruh catatan riwayat dan artefak tersimpan akan dihapus permanen.", onConfirm = { viewModel.clearHistory(); showClearDialog = false }, onDismiss = { showClearDialog = false })
    }
}
