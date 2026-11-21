"""Coordinates local vs GitHub Actions host-toolchain builds across all 6 build modes."""
from __future__ import annotations

from typing import Any

from .processing_coordinator import execute_transformation_pipeline

BUILD_MODES: dict[str, dict[str, Any]] = {
    "protected_python": {
        "id": "protected_python",
        "title": "Protected Python Source",
        "environment": "ON_DEVICE",
        "supported_targets": ["android-arm64", "linux-x86_64", "linux-arm64", "windows-x64"],
        "toolchain": "Embedded Chaquopy Python 3.13 + PyCryptodome",
        "output_format": ".py",
        "core_modules": ["obfuscator.py", "packer.py", "string_enc.py", "identifier_rename.py", "ast_integrity.py"],
    },
    "pyinstaller": {
        "id": "pyinstaller",
        "title": "PyInstaller Standalone Binary",
        "environment": "BUILD_BACKEND",
        "supported_targets": ["linux-x86_64", "linux-arm64", "windows-x64"],
        "toolchain": "Host Python + PyInstaller + PyCryptodome",
        "output_format": "ELF / EXE binary",
        "core_modules": ["obfuscator.py", "packer.py", "bootstrap.py", "leak_scan.py"],
    },
    "cython_native": {
        "id": "cython_native",
        "title": "Cython Native Build",
        "environment": "BUILD_BACKEND",
        "supported_targets": ["linux-x86_64", "linux-arm64", "android-arm64-ndk"],
        "toolchain": "Cython + GCC/Clang + libpython headers + cython_guard",
        "output_format": "Compiled Native Binary + Guard",
        "core_modules": ["obfuscator.py", "cython_build.py", "cython_guard.py", "bootstrap.py", "leak_scan.py"],
    },
    "embedded_build": {
        "id": "embedded_build",
        "title": "Embedded C Bytecode Build",
        "environment": "BUILD_BACKEND",
        "supported_targets": ["linux-x86_64", "linux-arm64", "android-arm64-ndk"],
        "toolchain": "GCC/Clang + libpython C API",
        "output_format": "Standalone Embedded C Binary",
        "core_modules": ["obfuscator.py", "embed_build.py", "crypto.py", "leak_scan.py"],
    },
    "native_launcher": {
        "id": "native_launcher",
        "title": "Native C Launcher Wrapper",
        "environment": "BUILD_BACKEND",
        "supported_targets": ["linux-x86_64", "linux-arm64", "android-arm64-ndk"],
        "toolchain": "GCC/Clang + XOR/SHA-256 payload wrapper",
        "output_format": "Native C Wrapper Binary",
        "core_modules": ["native_launcher.py", "crypto.py", "libpython_fetch.py"],
    },
    "rust_launcher": {
        "id": "rust_launcher",
        "title": "Rust Launcher Wrapper",
        "environment": "BUILD_BACKEND",
        "supported_targets": ["linux-x86_64", "linux-arm64", "windows-x64"],
        "toolchain": "rustc / cargo + AES-GCM + Ed25519 manifest",
        "output_format": "Native Rust Wrapper Binary",
        "core_modules": ["rust_launcher.py", "launch_integrity.py", "crypto.py"],
    },
}


def coordinate_build(project: dict[str, Any], options: dict[str, Any]) -> dict[str, Any]:
    mode_id = str(options.get("build_mode", "protected_python"))
    spec = BUILD_MODES.get(mode_id)
    if spec is None:
        return {
            "success": False,
            "error": {"code": "UNSUPPORTED_BUILD_MODE", "message": f"Build mode tidak dikenal: {mode_id}"},
        }

    target_platform = str(options.get("target_platform", "android-arm64"))
    if target_platform not in spec["supported_targets"]:
        return {
            "success": False,
            "error": {
                "code": "UNSUPPORTED_TARGET_ABI",
                "message": f"Target '{target_platform}' tidak didukung untuk mode '{spec['title']}'. "
                           f"Target valid: {', '.join(spec['supported_targets'])}",
            },
        }

    source = str(project.get("source", ""))
    filename = str(project.get("filename", "main.py"))

    if spec["environment"] == "ON_DEVICE":
        proc_opts = dict(options.get("transform_options") or {})
        proc_opts["output_mode"] = "py_launcher"
        if options.get("output_filename"):
            proc_opts["output_filename"] = options["output_filename"]
        res = execute_transformation_pipeline(source, proc_opts, filename=filename)
        res["build"] = {
            "mode": spec,
            "target_platform": target_platform,
            "execution_environment": "ON_DEVICE",
            "requires_github_actions": False,
        }
        return res

    prep_opts = dict(options.get("transform_options") or {})
    from .compatibility import check_pycryptodome_available
    use_cython_stage1 = mode_id in ("cython_native", "embedded_build") or not check_pycryptodome_available()
    prep_opts["output_mode"] = "cython_source" if use_cython_stage1 else "py_launcher"
    prepared = execute_transformation_pipeline(source, prep_opts, filename=filename)

    dispatch_inputs = {
        "build_mode": mode_id,
        "target_platform": target_platform,
        "output_filename": str(options.get("output_filename", "app_protected_bin")),
        "onefile": str(bool(options.get("onefile", True))).lower(),
        "console": str(bool(options.get("console", True))).lower(),
        "cython_guard": str(bool(options.get("cython_guard", True))).lower(),
        "wrap_mode": str(options.get("wrap_mode", "py")),
        "run_leak_scan": str(bool(options.get("run_leak_scan", True))).lower(),
        "sign_manifest": str(bool(options.get("sign_manifest", False))).lower(),
    }

    return {
        "success": True,
        "status": "prepared_for_build_backend",
        "execution_environment": "BUILD_BACKEND",
        "requires_github_actions": True,
        "workflow_file": ".github/workflows/core-build.yml",
        "mode": spec,
        "target_platform": target_platform,
        "dispatch_payload": {
            "ref": str(options.get("git_ref", "main")),
            "inputs": dispatch_inputs,
        },
        "prepared_stage1": {
            "filename": prepared["output"]["filename"],
            "sha256": prepared["output"]["sha256"],
            "size": prepared["output"]["size"],
            "source": prepared["output"]["source"],
        },
        "logs": prepared["logs"]
        + [
            f"[build_bridge] Mode '{spec['title']}' membutuhkan toolchain host ({spec['toolchain']}).",
            f"[build_bridge] Target: {target_platform} -> siap dikirim ke .github/workflows/core-build.yml",
        ],
    }
