package com.verticthub.pyobfuscator.engine

import com.verticthub.pyobfuscator.data.model.ProcessingProgress
import com.verticthub.pyobfuscator.data.model.ProcessingResult
import com.verticthub.pyobfuscator.data.model.ProjectWorkspace
import com.verticthub.pyobfuscator.data.model.TransformOptions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProcessingCoordinator(private val bridge: PythonEngineBridge, private val scope: CoroutineScope) {
    private val _progress = MutableStateFlow(ProcessingProgress())
    val progress: StateFlow<ProcessingProgress> = _progress.asStateFlow()
    private var activeJob: Job? = null

    fun startTransform(project: ProjectWorkspace, options: TransformOptions, onCompleted: (ProcessingResult) -> Unit) {
        if (activeJob?.isActive == true) return
        activeJob = scope.launch {
            val completed = mutableListOf<String>()
            val logs = mutableListOf<String>()
            try {
                logs.add("[coordinator] Memulai pipeline transformasi untuk ${project.filename}")
                _progress.value = ProcessingProgress(true, "Transformasi AST & Proteksi Source", project.filename, "ast_integrity.py", "Validasi AST Input & Konfigurasi", 0.20f, completed.toList(), logs.toList())
                val optionsJson = EngineResultMapper.optionsToJson(options)
                completed.add("Validasi AST Input")
                _progress.value = _progress.value.copy(activeModule = "obfuscator.py", currentStage = "Menjalankan Core Obfuscator & Packer", progress = 0.55f, completedStages = completed.toList(), logs = logs.toList())

                val rawJson = bridge.processProject(project.source, optionsJson, project.filename)
                if (!rawJson.optBoolean("success", false)) {
                    val err = rawJson.optJSONObject("error")
                    val code = err?.optString("code") ?: "ENGINE_ERROR"
                    val msg = err?.optString("message") ?: "Transformasi gagal."
                    logs.add("[ERROR] [$code] $msg")
                    _progress.value = _progress.value.copy(isRunning = false, currentStage = "Gagal", errorCode = code, errorMessage = msg, logs = logs.toList())
                    return@launch
                }
                completed.add("Transformasi Core & Validasi Output")
                completed.add("Leak Scan & Laporan Integritas")
                val parsed = EngineResultMapper.parseProcessingResult(rawJson, project.source)
                logs.addAll(parsed.logs)
                _progress.value = ProcessingProgress(false, "Transformasi Selesai", project.filename, "api.py", "Selesai", 1.0f, completed.toList(), logs.toList(), parsed.warnings)
                onCompleted(parsed)
            } catch (ce: CancellationException) {
                logs.add("[coordinator] Proses dibatalkan oleh pengguna.")
                _progress.value = _progress.value.copy(isRunning = false, currentStage = "Dibatalkan", logs = logs.toList())
            } catch (t: Throwable) {
                logs.add("[ERROR] ${t.javaClass.simpleName}: ${t.message}")
                _progress.value = _progress.value.copy(isRunning = false, currentStage = "Error", errorCode = "BRIDGE_EXCEPTION", errorMessage = t.message ?: "Kesalahan", logs = logs.toList())
            }
        }
    }

    fun cancelActiveTask() { activeJob?.cancel() }
}
