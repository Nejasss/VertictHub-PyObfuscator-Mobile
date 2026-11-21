package com.verticthub.pyobfuscator.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.verticthub.pyobfuscator.design.components.*
import com.verticthub.pyobfuscator.design.theme.LocalStudioPalette
import com.verticthub.pyobfuscator.ui.StudioUiState
import com.verticthub.pyobfuscator.ui.StudioViewModel

@Composable
fun ProgressScreen(state: StudioUiState, viewModel: StudioViewModel, onBack: () -> Unit) {
    val palette = LocalStudioPalette.current
    val prog = state.progress

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        VHSurfaceCard(elevated = true) {
            Text(prog.taskTitle.ifBlank { "Pipeline Transformasi" }, style = MaterialTheme.typography.titleMedium, color = palette.textPrimary)
            Text("File aktif: ${prog.inputFilename}", style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
            VHProgressIndicator(stageLabel = prog.currentStage.ifBlank { "Menunggu..." }, activeModule = prog.activeModule.ifBlank { "api.py" }, progressFraction = prog.progress)
        }

        if (prog.errorMessage != null) {
            VHErrorPanel(title = "Eksekusi Terhenti", message = prog.errorMessage, code = prog.errorCode, onDismiss = onBack)
        }

        VHSurfaceCard {
            VHSectionHeader(title = "Tahapan Selesai")
            if (prog.completedStages.isEmpty()) {
                Text("Sedang menjalankan tahap awal...", style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
            } else {
                prog.completedStages.forEach { st ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = palette.success, modifier = Modifier.size(16.dp))
                        Text(st, style = MaterialTheme.typography.bodySmall, color = palette.textPrimary)
                    }
                }
            }
        }

        VHSurfaceCard {
            VHSectionHeader(title = "Log Eksekusi Engine")
            VHLogViewer(lines = prog.logs)
        }

        if (prog.isRunning) {
            VHSecondaryButton(text = "Batalkan Proses", onClick = { viewModel.cancelActiveTask() }, icon = Icons.Default.Cancel, modifier = Modifier.fillMaxWidth())
        } else {
            VHPrimaryButton(text = "Kembali", onClick = onBack, modifier = Modifier.fillMaxWidth())
        }
    }
}
