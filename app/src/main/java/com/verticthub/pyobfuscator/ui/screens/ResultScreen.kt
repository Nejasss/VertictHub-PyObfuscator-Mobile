package com.verticthub.pyobfuscator.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.verticthub.pyobfuscator.design.components.*
import com.verticthub.pyobfuscator.design.theme.LocalStudioPalette
import com.verticthub.pyobfuscator.design.tokens.CodeTextStyle
import com.verticthub.pyobfuscator.ui.StudioUiState
import com.verticthub.pyobfuscator.ui.StudioViewModel
import com.verticthub.pyobfuscator.util.DiffLineType
import com.verticthub.pyobfuscator.util.LineDiffEngine

@Composable
fun ResultScreen(state: StudioUiState, viewModel: StudioViewModel) {
    val palette = LocalStudioPalette.current
    val context = LocalContext.current
    val res = state.latestResult
    var selectedTab by remember { mutableIntStateOf(0) }

    if (res == null) {
        Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            VHEmptyState(title = "Belum Ada Hasil", description = "Jalankan transformasi atau build terlebih dahulu untuk memeriksa artefak output dan laporan.")
        }
        return
    }

    val saveFileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/x-python")) { uri ->
        if (uri != null) viewModel.saveExportToUri(uri, res.transformedSource)
    }

    val saveReportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) viewModel.saveExportToUri(uri, res.rawJsonReport)
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        VHSurfaceCard(elevated = true) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(res.outputFilename, style = MaterialTheme.typography.titleMedium, color = palette.textPrimary)
                    Text("${res.outputSizeBytes} bytes • ${res.executionEnvironment}", style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
                }
                VHStatusBadge(label = if (res.success) "SUCCESS" else "FAILED", tone = if (res.success) BadgeTone.SUCCESS else BadgeTone.ERROR)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                VHStatItem("Core Loaded", "${res.modulesLoaded}/34", Modifier.weight(1f))
                VHStatItem("Leak Tokens", res.leakTotalCount.toString(), Modifier.weight(1f))
                VHStatItem("Delta Size", "${res.outputSizeBytes - res.inputSizeBytes} B", Modifier.weight(1f))
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                VHPrimaryButton(text = "Simpan File", onClick = { saveFileLauncher.launch(res.outputFilename) }, icon = Icons.Default.Save, modifier = Modifier.weight(1f))
                VHSecondaryButton(text = "Bagikan", onClick = {
                    val shareIntent = viewModel.safFileManager.createShareIntent(res.outputFilename, res.transformedSource)
                    context.startActivity(android.content.Intent.createChooser(shareIntent, "Bagikan Hasil"))
                }, icon = Icons.Default.Share, modifier = Modifier.weight(1f))
            }
            VHSecondaryButton(text = "Ekspor Laporan JSON", onClick = { saveReportLauncher.launch("${res.outputFilename}.report.json") }, modifier = Modifier.fillMaxWidth())
        }

        TabRow(selectedTabIndex = selectedTab, containerColor = palette.surface, contentColor = palette.primary) {
            listOf("Output", "Diff", "Laporan", "Log").forEachIndexed { idx, label ->
                Tab(selected = selectedTab == idx, onClick = { selectedTab = idx }, text = { Text(label) })
            }
        }

        when (selectedTab) {
            0 -> {
                VHSurfaceCard {
                    VHSectionHeader(title = "Kode Hasil Transformasi", subtitle = "SHA-256: ${res.outputSha256}")
                    VHCodePreview(source = res.transformedSource)
                }
            }
            1 -> {
                val diff = remember(res.originalSource, res.transformedSource) {
                    LineDiffEngine.computeDiff(res.originalSource, res.transformedSource)
                }
                VHSurfaceCard {
                    VHSectionHeader(title = "Perbandingan Baris", subtitle = "+${diff.addedCount} baris ditambah • -${diff.removedCount} baris dihapus")
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        diff.lines.take(250).forEach { dl ->
                            val bg = when (dl.type) {
                                DiffLineType.ADDED -> palette.success.copy(alpha = 0.14f)
                                DiffLineType.REMOVED -> palette.error.copy(alpha = 0.14f)
                                DiffLineType.UNCHANGED -> androidx.compose.ui.graphics.Color.Transparent
                            }
                            val prefix = when (dl.type) { DiffLineType.ADDED -> "+"; DiffLineType.REMOVED -> "-"; DiffLineType.UNCHANGED -> " " }
                            Text("$prefix ${dl.content}", style = CodeTextStyle, color = palette.textPrimary, modifier = Modifier.fillMaxWidth().background(bg).padding(horizontal = 4.dp, vertical = 1.dp))
                        }
                    }
                }
            }
            2 -> {
                VHSurfaceCard {
                    VHSectionHeader(title = "Laporan Integritas & Validasi")
                    Text("Validasi AST: ${res.validationSummary}", style = MaterialTheme.typography.bodySmall, color = palette.textPrimary)
                    Text("Manifest & Gate: ${res.manifestSummary}", style = MaterialTheme.typography.bodySmall, color = palette.textPrimary)
                    Text("Runtime: ${res.runtimeCompatibilitySummary}", style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
                    Text("Leak Scan:\n${res.leakFormattedReport}", style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                }
                if (res.transformations.isNotEmpty()) {
                    VHSurfaceCard {
                        VHSectionHeader(title = "Ringkasan Pass Transformasi")
                        res.transformations.forEach { pass ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(pass.label, style = MaterialTheme.typography.bodySmall, color = palette.textPrimary, modifier = Modifier.weight(1f))
                                VHStatusBadge(label = pass.status.uppercase(), tone = if (pass.enabled) BadgeTone.SUCCESS else BadgeTone.NEUTRAL)
                            }
                        }
                    }
                }
            }
            3 -> {
                VHSurfaceCard {
                    VHSectionHeader(title = "Log Eksekusi")
                    VHLogViewer(lines = res.logs)
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}
