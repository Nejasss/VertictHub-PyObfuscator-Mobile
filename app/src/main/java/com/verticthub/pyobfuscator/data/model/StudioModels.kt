package com.verticthub.pyobfuscator.data.model

import com.verticthub.pyobfuscator.design.theme.ThemeMode

data class ProjectWorkspace(
    val id: String,
    val projectName: String,
    val filename: String,
    val source: String,
    val sizeBytes: Int,
    val lineCount: Int,
    val encodingInfo: String = "UTF-8",
    val lineEndingInfo: String = "LF",
    val lastModifiedMillis: Long = System.currentTimeMillis(),
    val astValid: Boolean = true,
    val astNodeCount: Int = 0,
    val functionCount: Int = 0,
    val classCount: Int = 0,
    val detectedImports: List<String> = emptyList(),
    val supportedConstantsCount: Int = 0,
)

data class TransformOptions(
    val preset: String = "default_cli",
    val outputMode: String = "py_launcher",
    val validateInput: Boolean = true,
    val validateOutput: Boolean = true,
    val preserveDocstrings: Boolean = true,
    val runLeakScan: Boolean = true,
    val outputFilename: String = "protected_output.py",
    val encryptStrings: Boolean = true,
    val minStringLen: Int = 1,
    val renameIdentifiers: Boolean = true,
    val cjkIdentifiers: Boolean = false,
    val junkIntensity: Int = 6,
    val injectDecoyMethods: Boolean = true,
    val controlFlowObfuscation: Boolean = false,
    val constantProtection: Boolean = false,
    val expressionObfuscation: Boolean = false,
    val importObfuscation: Boolean = false,
    val attributeObfuscation: Boolean = false,
    val functionTransformation: Boolean = false,
    val dataFlowObfuscation: Boolean = false,
    val lockFilename: Boolean = false,
    val requireLauncherGate: Boolean = true,
    val sslPinSha256Csv: String = "",
    val signManifest: Boolean = false,
    val antiDebug: Boolean = true,
    val periodicRecheck: Boolean = false,
    val recheckIntervalMin: Int = 4,
    val recheckIntervalMax: Int = 9,
    val bytecodeIntegrityCheck: Boolean = false,
    val enableAntisandbox: Boolean = false,
    val antisandboxThreshold: Int = 6,
    val antiVpn: Boolean = false,
    val strictNetworkCheck: Boolean = false,
    val hookDetection: Boolean = false,
)

enum class BuildModeId(
    val id: String,
    val title: String,
    val isLocalOnDevice: Boolean,
    val supportedTargets: List<String>,
    val toolchainSummary: String,
    val outputFormat: String,
    val coreModules: String,
) {
    PROTECTED_PYTHON("protected_python", "Protected Python Source", true, listOf("android-arm64", "linux-x86_64", "linux-arm64", "windows-x64"), "Embedded Chaquopy Python 3.13 + PyCryptodome", ".py (AES-256-GCM Loader)", "obfuscator.py, packer.py, string_enc.py, identifier_rename.py"),
    PYINSTALLER("pyinstaller", "PyInstaller Standalone Binary", false, listOf("linux-x86_64", "linux-arm64", "windows-x64"), "Host Python + PyInstaller + PyCryptodome", "ELF / EXE Binary", "obfuscator.py, packer.py, bootstrap.py, build.py"),
    CYTHON_NATIVE("cython_native", "Cython Native Build", false, listOf("linux-x86_64", "linux-arm64", "android-arm64-ndk"), "Cython + GCC/Clang + libpython + cython_guard", "Compiled Native Binary + Guard", "obfuscator.py, cython_build.py, cython_guard.py, leak_scan.py"),
    EMBEDDED_BUILD("embedded_build", "Embedded C Bytecode Build", false, listOf("linux-x86_64", "linux-arm64", "android-arm64-ndk"), "GCC/Clang + libpython C API", "Standalone Embedded C Binary", "obfuscator.py, embed_build.py, crypto.py"),
    NATIVE_LAUNCHER("native_launcher", "Native C Launcher Wrapper", false, listOf("linux-x86_64", "linux-arm64", "android-arm64-ndk"), "GCC/Clang + XOR/SHA-256 Payload Wrapper", "Native C Wrapper Binary", "native_launcher.py, crypto.py, libpython_fetch.py"),
    RUST_LAUNCHER("rust_launcher", "Rust Launcher Wrapper", false, listOf("linux-x86_64", "linux-arm64", "windows-x64"), "rustc / cargo + AES-GCM + Ed25519 Manifest", "Native Rust Wrapper Binary", "rust_launcher.py, launch_integrity.py, crypto.py"),
}

data class BuildOptions(
    val buildMode: BuildModeId = BuildModeId.PROTECTED_PYTHON,
    val targetPlatform: String = "android-arm64",
    val outputFilename: String = "app_protected.py",
    val onefile: Boolean = true,
    val console: Boolean = true,
    val cythonGuard: Boolean = true,
    val wrapMode: String = "py",
    val runLeakScan: Boolean = true,
    val signManifest: Boolean = false,
    val autoRunAfterBuild: Boolean = false,
)

data class ProcessingProgress(
    val isRunning: Boolean = false,
    val taskTitle: String = "",
    val inputFilename: String = "",
    val activeModule: String = "",
    val currentStage: String = "",
    val progress: Float = 0f,
    val completedStages: List<String> = emptyList(),
    val logs: List<String> = emptyList(),
    val warnings: List<String> = emptyList(),
    val errorCode: String? = null,
    val errorMessage: String? = null,
)

data class TransformationPassSummary(val key: String, val label: String, val enabled: Boolean, val status: String, val detail: String)

data class ProcessingResult(
    val success: Boolean,
    val status: String,
    val executionEnvironment: String,
    val inputFilename: String,
    val inputSizeBytes: Int,
    val inputSha256: String,
    val outputFilename: String,
    val outputSizeBytes: Int,
    val outputSha256: String,
    val originalSource: String,
    val transformedSource: String,
    val modulesLoaded: Int,
    val modulesVerified: Int,
    val transformations: List<TransformationPassSummary>,
    val leakTotalCount: Int,
    val leakFormattedReport: String,
    val validationSummary: String,
    val manifestSummary: String,
    val runtimeCompatibilitySummary: String,
    val warnings: List<String>,
    val logs: List<String>,
    val rawJsonReport: String,
    val remoteWorkflowUrl: String? = null,
)

data class HistoryRecord(
    val id: String,
    val projectName: String,
    val sourceFilename: String,
    val timestampMillis: Long,
    val configSummary: String,
    val status: String,
    val buildMode: String,
    val validationStatus: String,
    val outputReference: String,
)

data class CoreModuleStatus(
    val filename: String,
    val module: String,
    val filePresent: Boolean,
    val sha256Verified: Boolean,
    val importOk: Boolean,
    val apisOk: Boolean,
    val dependencyStatus: String,
    val executionEnvironment: String,
    val compatibility: String,
    val status: String,
)

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.DARK,
    val defaultPreset: String = "default_cli",
    val autoCleanTempFiles: Boolean = true,
    val githubOwnerRepo: String = "",
    val githubBranch: String = "main",
    val githubTokenConfigured: Boolean = false,
    val defaultBuildTarget: String = "android-arm64",
    val signingKeyConfigured: Boolean = false,
)
