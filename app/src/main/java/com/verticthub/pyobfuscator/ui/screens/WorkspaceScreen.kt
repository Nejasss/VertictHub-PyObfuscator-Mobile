package com.verticthub.pyobfuscator.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.verticthub.pyobfuscator.design.components.*
import com.verticthub.pyobfuscator.design.theme.LocalStudioPalette
import com.verticthub.pyobfuscator.ui.StudioUiState
import com.verticthub.pyobfuscator.ui.StudioViewModel

@Composable
fun WorkspaceScreen(
    state: StudioUiState,
    viewModel: StudioViewModel,
    onNavigateObfuscate: () -> Unit,
    onNavigateBuild: () -> Unit,
    onNavigateResult: () -> Unit,
) {
    val palette = LocalStudioPalette.current
    var showPasteDialog by remember { mutableStateOf(false) }
    var pasteFilename by remember { mutableStateOf("script.py") }
    var pasteText by remember { mutableStateOf("") }
    var editMode by remember { mutableStateOf(false) }
    var editableSource by remember(state.workspace?.id) { mutableStateOf(state.workspace?.source.orEmpty()) }
    var searchQuery by remember { mutableStateOf("") }

    val openDocLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.importFileFromUri(uri)
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        VHSurfaceCard(elevated = true) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Engine Chaquopy ARM64", style = MaterialTheme.typography.titleMedium, color = palette.textPrimary)
                    Text("Python ${state.pythonVersion} • ${state.modulesLoaded}/34 modul inti • PyCryptodome ${if (state.pycryptodomeReady) "Aktif" else "Tidak Ada"}", style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
                }
                VHStatusBadge(label = "${state.modulesVerified}/34 VERIFIED", tone = if (state.modulesVerified == 34) BadgeTone.SUCCESS else BadgeTone.WARNING)
            }
        }

        VHSectionHeader(title = "Impor & Workspace Kode", subtitle = "Pilih file .py atau .zip via Android SAF, tempel kode, atau muat contoh")
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            VHPrimaryButton(text = "Buka File", onClick = { openDocLauncher.launch(arrayOf("*/*")) }, icon = Icons.Default.FolderOpen, modifier = Modifier.weight(1f))
            VHSecondaryButton(text = "Tempel Kode", onClick = { showPasteDialog = true }, icon = Icons.Default.ContentPaste, modifier = Modifier.weight(1f))
        }
        VHSecondaryButton(text = "Muat Contoh Proyek (demo_service.py)", onClick = { viewModel.loadSampleProject() }, icon = Icons.Default.PlayArrow, modifier = Modifier.fillMaxWidth())

        val ws = state.workspace
        if (ws == null) {
            VHEmptyState(title = "Belum Ada File Python Aktif", description = "Impor file .py atau arsip .zip dari penyimpanan perangkat untuk mulai menganalisis AST, mengonfigurasi proteksi, dan mengekspor hasil.", actionLabel = "Muat Contoh Proyek", onAction = { viewModel.loadSampleProject() })
        } else {
            VHSurfaceCard {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(ws.filename, style = MaterialTheme.typography.titleMedium, color = palette.textPrimary)
                        Text("${ws.encodingInfo} • ${ws.lineEndingInfo} • ${ws.sizeBytes} bytes", style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
                    }
                    VHStatusBadge(label = if (ws.astValid) "AST VALID" else "SYNTAX ERROR", tone = if (ws.astValid) BadgeTone.SUCCESS else BadgeTone.ERROR)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    VHStatItem("Baris", ws.lineCount.toString(), Modifier.weight(1f))
                    VHStatItem("AST Node", ws.astNodeCount.toString(), Modifier.weight(1f))
                    VHStatItem("Fungsi / Kelas", "${ws.functionCount}/${ws.classCount}", Modifier.weight(1f))
                }
                if (ws.detectedImports.isNotEmpty()) {
                    Text("Imports terdeteksi: ${ws.detectedImports.joinToString(", ")}", style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    VHSecondaryButton(text = if (editMode) "Simpan Edit" else "Edit Kode", onClick = {
                        if (editMode) viewModel.updateWorkspaceSource(editableSource)
                        editMode = !editMode
                    }, icon = Icons.Default.Edit, modifier = Modifier.weight(1f))
                    VHPrimaryButton(text = "Obfuscate", onClick = onNavigateObfuscate, icon = Icons.Default.Tune, modifier = Modifier.weight(1f))
                }
                VHSecondaryButton(text = "Lanjut ke Build Target", onClick = onNavigateBuild, icon = Icons.Default.Build, modifier = Modifier.fillMaxWidth())
            }

            VHSurfaceCard {
                VHSectionHeader(title = "Pratinjau Sumber Python", subtitle = if (editMode) "Mode edit langsung" else "Pratinjau bernomor baris (horizontal scroll)")
                if (!editMode) {
                    OutlinedTextField(value = searchQuery, onValueChange = { searchQuery = it }, label = { Text("Cari teks dalam kode...") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    VHCodePreview(source = ws.source, searchQuery = searchQuery)
                } else {
                    OutlinedTextField(value = editableSource, onValueChange = { editableSource = it }, textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace), modifier = Modifier.fillMaxWidth().heightIn(min = 240.dp, max = 420.dp))
                }
            }
        }

        if (state.latestResult != null) {
            VHFileCard(filename = state.latestResult.outputFilename, metadata = "${state.latestResult.outputSizeBytes} bytes • ${state.latestResult.executionEnvironment}", statusLabel = "LIHAT HASIL", statusTone = BadgeTone.PRIMARY, onClick = onNavigateResult)
        }
        Spacer(Modifier.height(24.dp))
    }

    if (showPasteDialog) {
        AlertDialog(
            onDismissRequest = { showPasteDialog = false }, containerColor = palette.surface,
            title = { Text("Tempel Kode Python", color = palette.textPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = pasteFilename, onValueChange = { pasteFilename = it }, label = { Text("Nama file (.py)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = pasteText, onValueChange = { pasteText = it }, label = { Text("Tempel kode Python di sini") }, modifier = Modifier.fillMaxWidth().height(200.dp))
                }
            },
            confirmButton = {
                VHPrimaryButton(text = "Gunakan Kode", onClick = {
                    if (pasteText.isNotBlank()) {
                        val fn = if (pasteFilename.endsWith(".py")) pasteFilename else "$pasteFilename.py"
                        viewModel.setWorkspaceFromSource(fn, pasteText)
                        showPasteDialog = false
                    }
                })
            },
            dismissButton = { VHSecondaryButton(text = "Batal", onClick = { showPasteDialog = false }) },
        )
    }
}
