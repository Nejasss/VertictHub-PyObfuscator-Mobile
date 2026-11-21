package com.verticthub.pyobfuscator.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.verticthub.pyobfuscator.data.model.BuildModeId
import com.verticthub.pyobfuscator.design.components.*
import com.verticthub.pyobfuscator.design.theme.LocalStudioPalette
import com.verticthub.pyobfuscator.ui.StudioUiState
import com.verticthub.pyobfuscator.ui.StudioViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BuildScreen(state: StudioUiState, viewModel: StudioViewModel, onNavigateResult: () -> Unit) {
    val palette = LocalStudioPalette.current
    val bOpts = state.buildOptions

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        VHSectionHeader(title = "Pilih Mode Packaging & Build", subtitle = "Mode lokal berjalan langsung di Android; mode native/binary dikirim ke GitHub Actions")

        BuildModeId.entries.forEach { mode ->
            val selected = bOpts.buildMode == mode
            Surface(
                modifier = Modifier.fillMaxWidth().clickable {
                    viewModel.updateBuildOptions {
                        it.copy(buildMode = mode, targetPlatform = mode.supportedTargets.first())
                    }
                },
                shape = RoundedCornerShape(10.dp),
                color = if (selected) palette.surfaceElevated else palette.surface,
                border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) palette.primary else palette.divider),
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(mode.title, style = MaterialTheme.typography.titleSmall, color = palette.textPrimary)
                        VHStatusBadge(label = if (mode.isLocalOnDevice) "ON-DEVICE" else "BUILD BACKEND", tone = if (mode.isLocalOnDevice) BadgeTone.SUCCESS else BadgeTone.PRIMARY)
                    }
                    Text("Toolchain: ${mode.toolchainSummary}", style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
                    Text("Output: ${mode.outputFormat} • Modul: ${mode.coreModules}", style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                }
            }
        }

        VHSurfaceCard {
            VHSectionHeader(title = "Target Arsitektur & Parameter Build")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                bOpts.buildMode.supportedTargets.forEach { abi ->
                    FilterChip(selected = bOpts.targetPlatform == abi, onClick = { viewModel.updateBuildOptions { it.copy(targetPlatform = abi) } }, label = { Text(abi) })
                }
            }
            OutlinedTextField(value = bOpts.outputFilename, onValueChange = { fn -> viewModel.updateBuildOptions { it.copy(outputFilename = fn) } }, label = { Text("Nama Artefak Output") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            VHSettingRow("Mode Onefile", "Gabungkan seluruh dependensi ke satu artefak", { Switch(checked = bOpts.onefile, onCheckedChange = { v -> viewModel.updateBuildOptions { it.copy(onefile = v) } }) })
            VHSettingRow("Cython Guard", "Sertakan modul proteksi Cython Guard", { Switch(checked = bOpts.cythonGuard, onCheckedChange = { v -> viewModel.updateBuildOptions { it.copy(cythonGuard = v) } }) })
            VHSettingRow("Post-Build Leak Scan", "Pindai artefak hasil build terhadap token bocor", { Switch(checked = bOpts.runLeakScan, onCheckedChange = { v -> viewModel.updateBuildOptions { it.copy(runLeakScan = v) } }) })
            VHSettingRow("Tanda Tangani Manifest (Ed25519)", "Buat launch_manifest.json bertanda tangan", { Switch(checked = bOpts.signManifest, onCheckedChange = { v -> viewModel.updateBuildOptions { it.copy(signManifest = v) } }) })
        }

        VHPrimaryButton(
            text = if (bOpts.buildMode.isLocalOnDevice) "Bangun Paket Python Lokal" else "Siapkan & Kirim ke Build Backend",
            onClick = { viewModel.startBuild(onNavigateResult) },
            enabled = state.workspace != null && !state.isBusy,
            icon = Icons.Default.Build,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(24.dp))
    }
}
