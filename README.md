# VertictHub PyObfuscator Mobile

Aplikasi **Android Studio** berbasis **Kotlin + Jetpack Compose + Chaquopy (Python 3.13 ARM64)** yang membungkus dan mengintegrasikan seluruh **34 modul inti (`vh/core`)** dari **VertictHub PyObfuscator**.

## Fitur Utama
- **Integrasi Penuh 34 Modul Inti `vh.core`**: Semua 34 modul asli tersimpan utuh di `app/src/main/python/vh/core/` dan diaudit secara otomatis pada saat aplikasi berjalan (`34/34 present`, `34/34 loaded`, `34/34 SHA-256 verified`).
- **On-Device Execution (Chaquopy ARM64)**: Validasi AST (`ast_integrity.py`), pemindaian kebocoran token (`leak_scan.py`), transformasi AST lengkap (`obfuscator.py`, `string_enc.py`, `identifier_rename.py`, `control_flow.py`, `constant_protection.py`, `expression_obfuscation.py`, `import_obfuscation.py`, `attribute_obfuscation.py`, `function_transformation.py`, `data_flow_obfuscation.py`, `junk.py`), pengemasan AES-256-GCM (`packer.py`), dan SPKI SHA-256 pinning (`ssl_pinning.py`) berjalan langsung di perangkat Android secara offline.
- **Build Backend Orchestration (GitHub Actions)**: Mode build native/binary (`pyinstaller`, `cython_native`, `embedded_build`, `native_launcher`, `rust_launcher`) menyiapkan source tahap 1 di perangkat dan mengirim `workflow_dispatch` ke `.github/workflows/core-build.yml`.
- **Android SAF & Penyimpanan Terenkripsi**: Impor `.py`/`.zip` dan simpan/bagikan hasil via Storage Access Framework (`OpenDocument`, `CreateDocument`, `FileProvider`), serta simpan GitHub PAT dan kunci signing Ed25519 menggunakan `EncryptedSharedPreferences`.
