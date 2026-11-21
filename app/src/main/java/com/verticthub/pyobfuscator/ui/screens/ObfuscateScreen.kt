package com.verticthub.pyobfuscator.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.verticthub.pyobfuscator.design.components.*
import com.verticthub.pyobfuscator.design.theme.LocalStudioPalette
import com.verticthub.pyobfuscator.ui.StudioUiState
import com.verticthub.pyobfuscator.ui.StudioViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ObfuscateScreen(state: StudioUiState, viewModel: StudioViewModel, onStartProgress: () -> Unit, onCompletedResult: () -> Unit) {
    val palette = LocalStudioPalette.current
    val opts = state.transformOptions
    var certInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        VHSurfaceCard(elevated = true) {
            VHSectionHeader(title = "Preset Konfigurasi Engine", subtitle = "Sesuaikan seluruh lapisan proteksi sesuai kebutuhan")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("default_cli" to "Default CLI", "source_safe" to "Source-Safe", "maximum_ast" to "Maximum AST", "cython_ready" to "Cython-Ready").forEach { (key, label) ->
                    FilterChip(selected = opts.preset == key, onClick = { viewModel.applyPreset(key) }, label = { Text(label) })
                }
            }
            OutlinedTextField(value = opts.outputFilename, onValueChange = { fn -> viewModel.updateTransformOptions { it.copy(outputFilename = fn) } }, label = { Text("Nama File Output") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        }

        VHSurfaceCard {
            VHSectionHeader(title = "1. Integritas & Validasi", subtitle = "ast_integrity.py & leak_scan.py")
            VHFeatureRow("Validasi AST Input & Output", "Memeriksa sintaks Python, __future__ imports, dan kontrak docstring.", "DEFAULT ON", "ON-DEVICE", opts.validateInput && opts.validateOutput, { v -> viewModel.updateTransformOptions { it.copy(validateInput = v, validateOutput = v) } }, "ast_integrity.py")
            VHFeatureRow("Pemindaian Kebocoran Token (Leak Scan)", "Memindai hasil output terhadap sisa identifier sensitif.", "DEFAULT ON", "ON-DEVICE", opts.runLeakScan, { v -> viewModel.updateTransformOptions { it.copy(runLeakScan = v) } }, "leak_scan.py")
        }

        VHSurfaceCard {
            VHSectionHeader(title = "2. Enkripsi & Penyamaran Identitas", subtitle = "string_enc.py, identifier_rename.py, unicode_names.py")
            VHFeatureRow("Enkripsi Literal String & F-String", "Mengenkripsi string menggunakan HKDF-SHA256 & XOR stream.", "DEFAULT ON", "ON-DEVICE", opts.encryptStrings, { v -> viewModel.updateTransformOptions { it.copy(encryptStrings = v) } }, "string_enc.py")
            VHFeatureRow("Pengubahan Nama Identifier", "Mengganti nama fungsi, kelas, dan variabel lokal secara konsisten.", "DEFAULT ON", "ON-DEVICE", opts.renameIdentifiers, { v -> viewModel.updateTransformOptions { it.copy(renameIdentifiers = v) } }, "identifier_rename.py")
            VHFeatureRow("Identifier CJK (Karakter Han)", "Menggunakan karakter Unicode CJK untuk pengacakan simbol.", "DEFAULT OFF", "ON-DEVICE", opts.cjkIdentifiers, { v -> viewModel.updateTransformOptions { it.copy(cjkIdentifiers = v) } }, "unicode_names.py", enabled = opts.renameIdentifiers)
        }

        VHSurfaceCard {
            VHSectionHeader(title = "3. Transformasi Struktur AST", subtitle = "junk.py, control_flow.py, constant_protection.py, expression_obfuscation.py, dll.")
            Text("Intensitas Kode Sampah (Junk Intensity): ${opts.junkIntensity}", style = MaterialTheme.typography.titleSmall, color = palette.textPrimary)
            Slider(value = opts.junkIntensity.toFloat(), onValueChange = { v -> viewModel.updateTransformOptions { it.copy(junkIntensity = v.toInt()) } }, valueRange = 0f..15f, steps = 14)
            VHFeatureRow("Injeksi Decoy Methods", "Menyisipkan 1–3 metode umpan ke dalam kelas lokal.", "DEFAULT ON", "ON-DEVICE", opts.injectDecoyMethods, { v -> viewModel.updateTransformOptions { it.copy(injectDecoyMethods = v) } }, "junk.py")
            VHFeatureRow("Control Flow Flattening", "Meratakan blok if/else tunggal menjadi state-machine dispatcher.", "DEFAULT OFF", "ON-DEVICE", opts.controlFlowObfuscation, { v -> viewModel.updateTransformOptions { it.copy(controlFlowObfuscation = v) } }, "control_flow.py")
            VHFeatureRow("Constant Protection", "Memecah konstanta angka, bytes, dan tuple ke tabel terenkripsi.", "DEFAULT OFF", "ON-DEVICE", opts.constantProtection, { v -> viewModel.updateTransformOptions { it.copy(constantProtection = v) } }, "constant_protection.py")
            VHFeatureRow("Expression Obfuscation", "Mengubah operasi aritmetika dan boolean menjadi ekspresi majemuk.", "DEFAULT OFF", "ON-DEVICE", opts.expressionObfuscation, { v -> viewModel.updateTransformOptions { it.copy(expressionObfuscation = v) } }, "expression_obfuscation.py")
            VHFeatureRow("Import Obfuscation", "Mengganti statement import menjadi pemuatan dinamis berbasis runtime.", "DEFAULT OFF", "ON-DEVICE", opts.importObfuscation, { v -> viewModel.updateTransformOptions { it.copy(importObfuscation = v) } }, "import_obfuscation.py")
            VHFeatureRow("Attribute Obfuscation", "Mengubah akses atribut objek menjadi getattr/setattr dinamis.", "DEFAULT OFF", "ON-DEVICE", opts.attributeObfuscation, { v -> viewModel.updateTransformOptions { it.copy(attributeObfuscation = v) } }, "attribute_obfuscation.py")
            VHFeatureRow("Function Prefix Transformation", "Menambahkan prefix acak pada definisi fungsi tingkat modul.", "DEFAULT OFF", "ON-DEVICE", opts.functionTransformation, { v -> viewModel.updateTransformOptions { it.copy(functionTransformation = v) } }, "function_transformation.py")
            VHFeatureRow("Data Flow Obfuscation", "Menyamarkan alur penugasan variabel dengan alias dan state perantara.", "DEFAULT OFF", "ON-DEVICE", opts.dataFlowObfuscation, { v -> viewModel.updateTransformOptions { it.copy(dataFlowObfuscation = v) } }, "data_flow_obfuscation.py")
        }

        VHSurfaceCard {
            VHSectionHeader(title = "4. Pengemasan, Gate & SSL Pinning", subtitle = "packer.py, launch_integrity.py, ssl_pinning.py")
            VHFeatureRow("Launcher Gate Token", "Menyematkan token gerbang lingkungan pada payload terenkripsi AES-256-GCM.", "DEFAULT ON", "ON-DEVICE", opts.requireLauncherGate, { v -> viewModel.updateTransformOptions { it.copy(requireLauncherGate = v) } }, "packer.py / worker_gate_compat.py", enabled = opts.outputMode != "py_launcher")
            VHFeatureRow("Kunci Nama File Output (Filename Lock)", "Memvalidasi nama file saat dieksekusi agar tidak dijalankan setelah diubah nama.", "DEFAULT OFF", "ON-DEVICE", opts.lockFilename, { v -> viewModel.updateTransformOptions { it.copy(lockFilename = v) } }, "obfuscator.py")
            OutlinedTextField(value = opts.sslPinSha256Csv, onValueChange = { csv -> viewModel.updateTransformOptions { it.copy(sslPinSha256Csv = csv) } }, label = { Text("SPKI SHA-256 Pins (opsional, pisahkan koma)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = certInput, onValueChange = { certInput = it }, label = { Text("Hitung Pin dari Sertifikat PEM / Base64 DER") }, modifier = Modifier.fillMaxWidth())
            VHSecondaryButton(text = "Hitung SPKI SHA-256 Pin", onClick = { if (certInput.isNotBlank()) viewModel.computeSslPinFromInput(certInput) }, icon = Icons.Default.Security, modifier = Modifier.fillMaxWidth())
            if (state.calculatedSslPin != null) {
                Text("Pin: ${state.calculatedSslPin}", style = MaterialTheme.typography.labelSmall, color = palette.success)
            }
        }

        VHSurfaceCard {
            VHSectionHeader(title = "5. Pemeriksaan Runtime & Diagnostik Perangkat", subtitle = "antidebug.py, antisandbox.py, hook_detection.py")
            VHFeatureRow("Anti-Debug Guard", "Menyematkan pemeriksaan debugger pada kode yang dihasilkan.", "DEFAULT ON", "ON-DEVICE", opts.antiDebug, { v -> viewModel.updateTransformOptions { it.copy(antiDebug = v) } }, "antidebug.py")
            VHFeatureRow("Periodic Recheck Thread", "Pemeriksaan berkala di latar belakang selama eksekusi target.", "DEFAULT OFF", "ON-DEVICE", opts.periodicRecheck, { v -> viewModel.updateTransformOptions { it.copy(periodicRecheck = v) } }, "obfuscator.py")
            VHFeatureRow("Bytecode Integrity Check", "Verifikasi integritas objek kode Python saat runtime.", "DEFAULT OFF", "ON-DEVICE", opts.bytecodeIntegrityCheck, { v -> viewModel.updateTransformOptions { it.copy(bytecodeIntegrityCheck = v) } }, "obfuscator.py")
            VHFeatureRow("Anti-Sandbox Heuristic", "Deteksi lingkungan analisis dengan ambang skor=${opts.antisandboxThreshold}.", "DEFAULT OFF", "ON-DEVICE", opts.enableAntisandbox, { v -> viewModel.updateTransformOptions { it.copy(enableAntisandbox = v) } }, "antisandbox.py")
            VHFeatureRow("Anti-VPN / Network Guard", "Pemeriksaan jaringan eksplisit saat kode dijalankan.", "DEFAULT OFF", "ON-DEVICE", opts.antiVpn, { v -> viewModel.updateTransformOptions { it.copy(antiVpn = v) } }, "antidebug.py")
            VHFeatureRow("HTTP Hook Detection", "Mendeteksi monkey-patch pada klien HTTP Python.", "DEFAULT OFF", "ON-DEVICE", opts.hookDetection, { v -> viewModel.updateTransformOptions { it.copy(hookDetection = v) } }, "hook_detection.py")
            VHSecondaryButton(text = "Uji Diagnostik Lingkungan Perangkat (Read-Only)", onClick = { viewModel.runDiagnosticsProbe() }, modifier = Modifier.fillMaxWidth())
            state.diagnosticReport?.let { rep ->
                rep.findings.forEach { f ->
                    Text("• [${f.module}] ${f.check}: ${f.detail}", style = MaterialTheme.typography.labelSmall, color = if (f.triggered) palette.warning else palette.success)
                }
            }
        }

        VHPrimaryButton(text = "Proses & Obfuscate Sekarang", onClick = { viewModel.startTransformation(onStartProgress, onCompletedResult) }, enabled = state.workspace != null && !state.isBusy, icon = Icons.Default.PlayArrow, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(24.dp))
    }
}
