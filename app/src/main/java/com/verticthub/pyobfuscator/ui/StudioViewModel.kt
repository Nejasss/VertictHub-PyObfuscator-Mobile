package com.verticthub.pyobfuscator.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.verticthub.pyobfuscator.data.model.*
import com.verticthub.pyobfuscator.data.repo.StudioRepository
import com.verticthub.pyobfuscator.design.theme.ThemeMode
import com.verticthub.pyobfuscator.engine.*
import com.verticthub.pyobfuscator.storage.SafFileManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

data class StudioUiState(
    val engineReady: Boolean = false,
    val pythonVersion: String = "3.13",
    val pycryptodomeReady: Boolean = false,
    val modulesLoaded: Int = 0,
    val modulesVerified: Int = 0,
    val coreModules: List<CoreModuleStatus> = emptyList(),
    val workspace: ProjectWorkspace? = null,
    val transformOptions: TransformOptions = TransformOptions(),
    val buildOptions: BuildOptions = BuildOptions(),
    val progress: ProcessingProgress = ProcessingProgress(),
    val latestResult: ProcessingResult? = null,
    val history: List<HistoryRecord> = emptyList(),
    val settings: AppSettings = AppSettings(),
    val diagnosticReport: DiagnosticReport? = null,
    val calculatedSslPin: String? = null,
    val bannerMessage: String? = null,
    val errorBanner: String? = null,
    val isBusy: Boolean = false,
)

class StudioViewModel(application: Application) : AndroidViewModel(application) {
    private val runtimeManager = PythonRuntimeManager(application)
    private val bridge = PythonEngineBridge(runtimeManager)
    private val moduleRegistry = CoreModuleRegistry(bridge)
    private val processingCoordinator = ProcessingCoordinator(bridge, viewModelScope)
    private val buildCoordinator = BuildCoordinator(bridge)
    private val diagnostics = EngineDiagnostics(bridge)
    private val repository = StudioRepository(application)
    val safFileManager = SafFileManager(application)

    private val _uiState = MutableStateFlow(
        StudioUiState(settings = repository.loadSettings(), history = repository.loadHistory())
    )
    val uiState: StateFlow<StudioUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            processingCoordinator.progress.collect { prog ->
                _uiState.update { it.copy(progress = prog, isBusy = prog.isRunning) }
            }
        }
        initializeEngine()
    }

    fun initializeEngine() {
        viewModelScope.launch {
            val started = runtimeManager.ensureStarted()
            if (!started) {
                _uiState.update { it.copy(engineReady = false, errorBanner = runtimeManager.state.value.error ?: "Gagal memulai Chaquopy Python.") }
                return@launch
            }
            runCatching {
                val status = bridge.getEngineStatus()
                moduleRegistry.refresh()
                _uiState.update {
                    it.copy(
                        engineReady = true,
                        pythonVersion = status.optString("python_version", "3.13"),
                        pycryptodomeReady = status.optBoolean("pycryptodome_available", false),
                        modulesLoaded = status.optInt("modules_loaded", 0),
                        modulesVerified = status.optInt("modules_verified", 0),
                        coreModules = moduleRegistry.modules.value,
                        errorBanner = null,
                    )
                }
            }.onFailure { err -> _uiState.update { it.copy(engineReady = false, errorBanner = err.message) } }
        }
    }

    fun loadSampleProject() {
        val sample = """# Sample Python service module for VertictHub PyObfuscator
from __future__ import annotations
import hashlib

APP_BANNER = "VertictHub Mobile Studio Demo"
DEFAULT_ITERATIONS = 4096

class SessionValidator:
    def __init__(self, namespace: str) -> None:
        self.namespace = namespace

    def compute_digest(self, payload: str) -> str:
        raw = f"{self.namespace}:{payload}:{DEFAULT_ITERATIONS}".encode("utf-8")
        return hashlib.sha256(raw).hexdigest()

def run_demo() -> str:
    validator = SessionValidator("vh-studio")
    token = validator.compute_digest("sample-payload")
    return f"{APP_BANNER} -> {token[:16]}"

if __name__ == "__main__":
    print(run_demo())
"""
        setWorkspaceFromSource("demo_service.py", sample, sample.toByteArray().size, "UTF-8", "LF")
    }

    fun importFileFromUri(uri: Uri) {
        viewModelScope.launch {
            safFileManager.readPythonOrZipFromUri(uri).fold(
                onSuccess = { imported ->
                    setWorkspaceFromSource(imported.filename, imported.source, imported.sizeBytes, imported.encodingInfo, imported.lineEndingInfo)
                    _uiState.update { it.copy(bannerMessage = "Berhasil mengimpor ${imported.filename}", errorBanner = null) }
                },
                onFailure = { err -> _uiState.update { it.copy(errorBanner = err.message ?: "Gagal membaca file.") } },
            )
        }
    }

    fun setWorkspaceFromSource(filename: String, source: String, sizeBytes: Int = source.toByteArray().size, encodingInfo: String = "UTF-8", lineEndingInfo: String = "LF") {
        viewModelScope.launch {
            val baseName = filename.substringBeforeLast(".").ifBlank { "project" }
            val suggestedOut = "${baseName}_protected.py"
            if (_uiState.value.engineReady) {
                runCatching { bridge.analyzeProject(source, filename) }.onSuccess { analysis ->
                    val success = analysis.optBoolean("success", false)
                    val astObj = analysis.optJSONObject("ast")
                    val constObj = analysis.optJSONObject("constant_inventory")
                    val imports = mutableListOf<String>()
                    analysis.optJSONArray("detected_imports")?.let { arr -> for (i in 0 until arr.length()) imports.add(arr.getString(i)) }
                    _uiState.update { state ->
                        state.copy(
                            workspace = ProjectWorkspace(
                                id = UUID.randomUUID().toString(), projectName = baseName, filename = filename, source = source,
                                sizeBytes = sizeBytes, lineCount = source.lines().size, encodingInfo = encodingInfo, lineEndingInfo = lineEndingInfo,
                                astValid = success, astNodeCount = astObj?.optInt("node_count") ?: 0,
                                functionCount = astObj?.optInt("function_count") ?: 0, classCount = astObj?.optInt("class_count") ?: 0,
                                detectedImports = imports, supportedConstantsCount = constObj?.optInt("total_supported_literals") ?: 0,
                            ),
                            transformOptions = state.transformOptions.copy(outputFilename = suggestedOut),
                            buildOptions = state.buildOptions.copy(outputFilename = suggestedOut),
                            errorBanner = if (success) null else analysis.optJSONObject("error")?.optString("message"),
                        )
                    }
                    return@launch
                }
            }
            _uiState.update { state ->
                state.copy(
                    workspace = ProjectWorkspace(UUID.randomUUID().toString(), baseName, filename, source, sizeBytes, source.lines().size, encodingInfo, lineEndingInfo),
                    transformOptions = state.transformOptions.copy(outputFilename = suggestedOut),
                    buildOptions = state.buildOptions.copy(outputFilename = suggestedOut),
                )
            }
        }
    }

    fun updateWorkspaceSource(newSource: String) {
        val current = _uiState.value.workspace ?: return
        setWorkspaceFromSource(current.filename, newSource, newSource.toByteArray().size, current.encodingInfo, current.lineEndingInfo)
    }

    fun updateTransformOptions(updater: (TransformOptions) -> TransformOptions) {
        _uiState.update { it.copy(transformOptions = updater(it.transformOptions)) }
    }

    fun applyPreset(presetKey: String) {
        val updated = when (presetKey) {
            "source_safe" -> _uiState.value.transformOptions.copy(preset = "source_safe", outputMode = "py_launcher", encryptStrings = true, minStringLen = 2, junkIntensity = 3, antiDebug = false, renameIdentifiers = true, injectDecoyMethods = false, periodicRecheck = false, controlFlowObfuscation = false, constantProtection = true, expressionObfuscation = true, importObfuscation = false, attributeObfuscation = false, functionTransformation = false, dataFlowObfuscation = false)
            "maximum_ast" -> _uiState.value.transformOptions.copy(preset = "maximum_ast", outputMode = "py_launcher", encryptStrings = true, minStringLen = 1, junkIntensity = 8, antiDebug = true, renameIdentifiers = true, cjkIdentifiers = true, lockFilename = true, injectDecoyMethods = true, periodicRecheck = true, controlFlowObfuscation = true, constantProtection = true, expressionObfuscation = true, importObfuscation = true, attributeObfuscation = true, functionTransformation = true, dataFlowObfuscation = true)
            "cython_ready" -> _uiState.value.transformOptions.copy(preset = "cython_ready", outputMode = "cython_source", encryptStrings = true, minStringLen = 1, junkIntensity = 5, antiDebug = true, renameIdentifiers = true, cjkIdentifiers = false, lockFilename = false, injectDecoyMethods = true, periodicRecheck = true, controlFlowObfuscation = false, constantProtection = true, expressionObfuscation = true, importObfuscation = true, attributeObfuscation = true, functionTransformation = true, dataFlowObfuscation = true)
            else -> TransformOptions(outputFilename = _uiState.value.transformOptions.outputFilename)
        }
        _uiState.update { it.copy(transformOptions = updated, bannerMessage = "Preset '$presetKey' diterapkan.") }
    }

    fun updateBuildOptions(updater: (BuildOptions) -> BuildOptions) {
        _uiState.update { it.copy(buildOptions = updater(it.buildOptions)) }
    }

    fun startTransformation(onNavigateToProgress: () -> Unit, onCompletedNavigateResult: () -> Unit) {
        val ws = _uiState.value.workspace ?: run {
            _uiState.update { it.copy(errorBanner = "Silakan pilih atau tempel file Python terlebih dahulu.") }
            return
        }
        onNavigateToProgress()
        processingCoordinator.startTransform(ws, _uiState.value.transformOptions) { result ->
            val recId = UUID.randomUUID().toString().take(12)
            repository.appendHistory(
                HistoryRecord(recId, ws.projectName, ws.filename, System.currentTimeMillis(), "Preset: ${_uiState.value.transformOptions.preset} | Mode: ${_uiState.value.transformOptions.outputMode}", if (result.success) "SUCCESS" else "FAILED", "ON_DEVICE", result.validationSummary, result.outputFilename),
                outputContent = result.transformedSource,
            )
            _uiState.update { it.copy(latestResult = result, history = repository.loadHistory()) }
            onCompletedNavigateResult()
        }
    }

    fun startBuild(onNavigateResult: () -> Unit) {
        val ws = _uiState.value.workspace ?: run {
            _uiState.update { it.copy(errorBanner = "Workspace kosong. Muat file Python terlebih dahulu.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, errorBanner = null) }
            val res = buildCoordinator.executeBuild(ws, _uiState.value.transformOptions, _uiState.value.buildOptions, _uiState.value.settings, repository.getGitHubToken())
            res.fold(
                onSuccess = { buildResult ->
                    val recId = UUID.randomUUID().toString().take(12)
                    repository.appendHistory(
                        HistoryRecord(recId, ws.projectName, ws.filename, System.currentTimeMillis(), "Build: ${_uiState.value.buildOptions.buildMode.title} (${_uiState.value.buildOptions.targetPlatform})", buildResult.status.uppercase(), _uiState.value.buildOptions.buildMode.id, buildResult.validationSummary, buildResult.outputFilename),
                        outputContent = buildResult.transformedSource,
                    )
                    _uiState.update { it.copy(isBusy = false, latestResult = buildResult, history = repository.loadHistory()) }
                    onNavigateResult()
                },
                onFailure = { err -> _uiState.update { it.copy(isBusy = false, errorBanner = err.message ?: "Build gagal.") } },
            )
        }
    }

    fun cancelActiveTask() { processingCoordinator.cancelActiveTask() }

    fun runDiagnosticsProbe() {
        viewModelScope.launch {
            val opts = _uiState.value.transformOptions
            diagnostics.runTransparentProbe(opts.antiDebug, opts.enableAntisandbox, opts.hookDetection, opts.antisandboxThreshold).fold(
                onSuccess = { rep -> _uiState.update { it.copy(diagnosticReport = rep, errorBanner = null) } },
                onFailure = { err -> _uiState.update { it.copy(errorBanner = err.message) } },
            )
        }
    }

    fun computeSslPinFromInput(certPemOrDerB64: String) {
        viewModelScope.launch {
            diagnostics.calculateSpkiPin(certPemOrDerB64).fold(
                onSuccess = { pin -> _uiState.update { it.copy(calculatedSslPin = pin, errorBanner = null) } },
                onFailure = { err -> _uiState.update { it.copy(errorBanner = err.message) } },
            )
        }
    }

    fun saveExportToUri(uri: Uri, content: String) {
        safFileManager.writeTextToUri(uri, content).fold(
            onSuccess = { bytes -> _uiState.update { it.copy(bannerMessage = "Tersimpan ($bytes bytes).") } },
            onFailure = { err -> _uiState.update { it.copy(errorBanner = err.message) } },
        )
    }

    fun openHistoryRecord(record: HistoryRecord, onOpenResult: () -> Unit) {
        val savedSource = repository.readArtifactContent(record.outputReference) ?: "# Artefak tersimpan di: ${record.outputReference}"
        val synthetic = ProcessingResult(
            success = record.status == "SUCCESS" || record.status.contains("PREPARED"), status = record.status,
            executionEnvironment = record.buildMode, inputFilename = record.sourceFilename, inputSizeBytes = 0, inputSha256 = "-",
            outputFilename = record.outputReference.substringAfterLast("/"), outputSizeBytes = savedSource.toByteArray().size,
            outputSha256 = "-", originalSource = _uiState.value.workspace?.source ?: "", transformedSource = savedSource,
            modulesLoaded = _uiState.value.modulesLoaded, modulesVerified = _uiState.value.modulesVerified,
            transformations = emptyList(), leakTotalCount = 0, leakFormattedReport = "Dimuat dari riwayat lokal.",
            validationSummary = record.validationStatus, manifestSummary = record.configSummary,
            runtimeCompatibilitySummary = "Arsip Riwayat Lokal", warnings = emptyList(),
            logs = listOf("[history] Membuka arsip ${record.id}"), rawJsonReport = "{}",
        )
        _uiState.update { it.copy(latestResult = synthetic) }
        onOpenResult()
    }

    fun deleteHistoryItem(id: String) { repository.deleteHistoryItem(id); _uiState.update { it.copy(history = repository.loadHistory()) } }
    fun clearHistory() { repository.clearAllHistory(); _uiState.update { it.copy(history = emptyList(), bannerMessage = "Riwayat dibersihkan.") } }
    fun clearTempCache() { val freed = safFileManager.clearTemporaryExports(); _uiState.update { it.copy(bannerMessage = "Cache sementara dibersihkan ($freed bytes).") } }

    fun updateSettings(updater: (AppSettings) -> AppSettings) {
        val next = updater(_uiState.value.settings)
        repository.saveSettings(next)
        _uiState.update { it.copy(settings = next) }
    }

    fun saveGitHubToken(token: String) {
        repository.saveGitHubToken(token)
        updateSettings { it.copy(githubTokenConfigured = token.isNotBlank()) }
        _uiState.update { it.copy(bannerMessage = if (token.isNotBlank()) "Token GitHub disimpan terenkripsi." else "Token GitHub dihapus.") }
    }

    fun saveSigningPrivateKey(pem: String) {
        repository.saveSigningPrivateKey(pem)
        updateSettings { it.copy(signingKeyConfigured = pem.isNotBlank()) }
        _uiState.update { it.copy(bannerMessage = if (pem.isNotBlank()) "Kunci penandatanganan disimpan terenkripsi." else "Kunci dihapus.") }
    }

    fun setThemeMode(mode: ThemeMode) { updateSettings { it.copy(themeMode = mode) } }
    fun clearBanners() { _uiState.update { it.copy(bannerMessage = null, errorBanner = null) } }
}
