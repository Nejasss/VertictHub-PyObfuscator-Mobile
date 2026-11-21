"""Audits all 34 original vh.core modules for presence, importability, and execution environment."""
from __future__ import annotations

import hashlib
import importlib
import json
from pathlib import Path
from typing import Any

MANDATORY_CORE_MODULES: list[tuple[str, str, str, list[str]]] = [
    ("__init__.py", "ON_DEVICE", "COMPATIBLE", []),
    ("antidebug.py", "ON_DEVICE", "ADAPTED", ["get_antidebug_source"]),
    ("antisandbox.py", "ON_DEVICE", "ADAPTED", ["get_antisandbox_source"]),
    ("ast_integrity.py", "ON_DEVICE", "COMPATIBLE", ["validate_source", "capture_docstring_contract"]),
    ("attribute_obfuscation.py", "ON_DEVICE", "COMPATIBLE", ["obfuscate_attributes"]),
    ("bootstrap.py", "BUILD_BACKEND", "HOST_DEPENDENT", ["wrap_binary_as_py"]),
    ("constant_protection.py", "ON_DEVICE", "COMPATIBLE", ["protect_constants", "scan_constants"]),
    ("control_flow.py", "ON_DEVICE", "COMPATIBLE", ["obfuscate_control_flow"]),
    ("crypto.py", "ON_DEVICE", "COMPATIBLE", ["derive_keystream", "xor_encrypt", "hkdf_sha256", "split_key_with_junk"]),
    ("cython_build.py", "BUILD_BACKEND", "HOST_DEPENDENT", ["compile_pyx_to_binary", "ensure_toolchain"]),
    ("cython_guard.py", "BUILD_BACKEND", "HOST_DEPENDENT", ["build_guard", "ensure_toolchain"]),
    ("data_flow_obfuscation.py", "ON_DEVICE", "COMPATIBLE", ["obfuscate_data_flow"]),
    ("embed_build.py", "BUILD_BACKEND", "HOST_DEPENDENT", ["compile_source_to_binary", "ensure_toolchain"]),
    ("expression_obfuscation.py", "ON_DEVICE", "COMPATIBLE", ["obfuscate_expressions"]),
    ("function_transformation.py", "ON_DEVICE", "COMPATIBLE", ["transform_functions"]),
    ("hook_detection.py", "ON_DEVICE", "ADAPTED", ["get_hook_detection_source"]),
    ("identifier_rename.py", "ON_DEVICE", "COMPATIBLE", ["rename_identifiers"]),
    ("import_obfuscation.py", "ON_DEVICE", "COMPATIBLE", ["obfuscate_imports"]),
    ("junk.py", "ON_DEVICE", "COMPATIBLE", ["generate_junk_block", "inject_decoy_methods"]),
    ("launch_integrity.py", "ON_DEVICE", "COMPATIBLE", ["build_manifest", "sign_manifest", "verify_signed_manifest"]),
    ("leak_scan.py", "ON_DEVICE", "COMPATIBLE", ["extract_sensitive_vocabulary", "scan_binary_for_leaks"]),
    ("libpython_fetch.py", "BUILD_BACKEND", "HOST_DEPENDENT", ["main"]),
    ("native_decryptor_build.py", "BUILD_BACKEND", "HOST_DEPENDENT", ["build_native_decryptor"]),
    ("native_launcher.py", "BUILD_BACKEND", "HOST_DEPENDENT", ["build_native_launcher", "ensure_toolchain"]),
    ("native_worker_compat.py", "ON_DEVICE", "COMPATIBLE", ["patch_native_worker_reentry", "patch_runtime_worker_entrypoint"]),
    ("obfuscator.py", "ON_DEVICE", "COMPATIBLE", ["ObfuscationConfig", "obfuscate_source", "obfuscate_source_for_cython"]),
    ("packer.py", "ON_DEVICE", "COMPATIBLE", ["pack", "extract_imported_modules"]),
    ("runtime_fetch.py", "BUILD_BACKEND", "HOST_DEPENDENT", ["ensure_python", "find_cached_python"]),
    ("rust_launcher.py", "BUILD_BACKEND", "HOST_DEPENDENT", ["build_rust_launcher"]),
    ("signing_autosetup.py", "ON_DEVICE", "ADAPTED", ["ensure_signing_key"]),
    ("ssl_pinning.py", "ON_DEVICE", "COMPATIBLE", ["compute_pin_from_der", "get_ssl_pinning_source"]),
    ("string_enc.py", "ON_DEVICE", "COMPATIBLE", ["encrypt_strings", "build_runtime_header"]),
    ("unicode_names.py", "ON_DEVICE", "COMPATIBLE", ["rand_cjk_identifier", "make_identifier_generator"]),
    ("worker_gate_compat.py", "ON_DEVICE", "COMPATIBLE", ["patch_worker_gate_environment"]),
]


def _vh_root() -> Path:
    return Path(__file__).resolve().parent.parent


def _load_recorded_manifest() -> dict[str, dict[str, Any]]:
    manifest_path = _vh_root() / "core_manifest.json"
    if not manifest_path.is_file():
        return {}
    try:
        parsed = json.loads(manifest_path.read_text(encoding="utf-8"))
        return parsed.get("manifest", {}).get("files", {})
    except Exception:
        return {}


def check_pycryptodome_available() -> bool:
    try:
        import Crypto.Cipher.AES  # noqa: F401
        import Crypto.PublicKey.ECC  # noqa: F401
        import Crypto.Signature.eddsa  # noqa: F401
        return True
    except ImportError:
        return False


def audit_core_modules() -> dict[str, Any]:
    core_dir = _vh_root() / "core"
    recorded = _load_recorded_manifest()
    has_crypto = check_pycryptodome_available()

    modules_out: list[dict[str, Any]] = []
    present_count = 0
    imported_count = 0
    verified_count = 0

    for filename, env, compat, expected_apis in MANDATORY_CORE_MODULES:
        file_path = core_dir / filename
        file_present = file_path.is_file()
        sha_match = False
        size_bytes = 0
        if file_present:
            present_count += 1
            raw = file_path.read_bytes()
            size_bytes = len(raw)
            actual_sha = hashlib.sha256(raw).hexdigest()
            rec = recorded.get(filename)
            sha_match = bool(rec and rec.get("sha256") == actual_sha)

        mod_name = "vh.core" if filename == "__init__.py" else f"vh.core.{filename[:-3]}"
        import_ok = False
        import_error = None
        apis_ok = False
        missing_apis: list[str] = []

        if file_present:
            try:
                mod = importlib.import_module(mod_name)
                import_ok = True
                imported_count += 1
                for api_name in expected_apis:
                    if not hasattr(mod, api_name):
                        missing_apis.append(api_name)
                apis_ok = len(missing_apis) == 0
            except Exception as exc:
                import_error = f"{type(exc).__name__}: {exc}"

        dep_status = "OK"
        if filename in ("packer.py", "launch_integrity.py", "rust_launcher.py") and not has_crypto:
            dep_status = "MISSING_PYCRYPTODOME"
        elif env == "BUILD_BACKEND":
            dep_status = "HOST_TOOLCHAIN_REQUIRED"

        if file_present and import_ok and apis_ok:
            verified_count += 1

        if not file_present:
            status = "MISSING"
        elif env == "BUILD_BACKEND":
            status = "BUILD_BACKEND_INTEGRATED"
        elif compat == "ADAPTED":
            status = "ANDROID_ADAPTED"
        elif import_ok and apis_ok and sha_match:
            status = "VERIFIED"
        elif import_ok and apis_ok:
            status = "INTEGRATED"
        else:
            status = "PRESERVED"

        modules_out.append({
            "filename": filename,
            "module": mod_name,
            "file_present": file_present,
            "size_bytes": size_bytes,
            "sha256_verified": sha_match,
            "import_ok": import_ok,
            "import_error": import_error,
            "apis_ok": apis_ok,
            "missing_apis": missing_apis,
            "dependency_status": dep_status,
            "execution_environment": env,
            "compatibility": compat,
            "status": status,
        })

    return {
        "total_expected": len(MANDATORY_CORE_MODULES),
        "modules_present": present_count,
        "modules_loaded": imported_count,
        "modules_verified": verified_count,
        "pycryptodome_available": has_crypto,
        "modules": modules_out,
    }
