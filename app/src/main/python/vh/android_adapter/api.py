"""Public JSON-serializable Engine API invoked by Kotlin PythonEngineBridge."""
from __future__ import annotations

import json
import platform
from typing import Any

from .build_bridge import BUILD_MODES, coordinate_build
from .compatibility import audit_core_modules, check_pycryptodome_available
from .diagnostics_bridge import (
    collect_transparent_environment_report,
    compute_spki_pin_from_pem_or_der,
    verify_diagnostic_generators,
)
from .exceptions import AdapterError
from .processing_coordinator import analyze_python_source, execute_transformation_pipeline
from vh.core import ast_integrity, leak_scan


def analyze_project(source: str, filename: str = "input.py") -> dict[str, Any]:
    try:
        return analyze_python_source(source, filename=filename)
    except AdapterError as exc:
        return {"success": False, "error": exc.to_dict()}


def process_project(source: str, options: dict[str, Any] | None = None, filename: str = "main.py") -> dict[str, Any]:
    try:
        return execute_transformation_pipeline(source, raw_options=options, filename=filename)
    except AdapterError as exc:
        return {"success": False, "status": "failed", "error": exc.to_dict()}


def build_project(project: dict[str, Any], options: dict[str, Any]) -> dict[str, Any]:
    try:
        return coordinate_build(project, options)
    except AdapterError as exc:
        return {"success": False, "status": "failed", "error": exc.to_dict()}


def validate_output(source: str, filename: str = "output.py") -> dict[str, Any]:
    try:
        rep = ast_integrity.validate_source(source, stage="explicit-validation", filename=filename)
        return {
            "success": True,
            "valid": True,
            "node_count": rep.node_count,
            "function_count": rep.function_count,
            "class_count": rep.class_count,
            "import_count": rep.import_count,
            "future_import_count": rep.future_import_count,
        }
    except ast_integrity.ASTIntegrityError as exc:
        return {"success": False, "valid": False, "error": str(exc)}


def scan_output(path: str, vocabulary: dict[str, Any] | None = None, source_for_vocab: str = "") -> dict[str, Any]:
    vocab = vocabulary if vocabulary is not None else leak_scan.extract_sensitive_vocabulary(source_for_vocab)
    raw = leak_scan.scan_binary_for_leaks(path, vocab)
    return {
        "success": True,
        "leaked_names": raw.get("leaked_names", []),
        "leaked_strings": raw.get("leaked_strings", []),
        "total_leaks": len(raw.get("leaked_names", [])) + len(raw.get("leaked_strings", [])),
        "formatted_report": leak_scan.format_report(raw, binary_label=path),
    }


def get_engine_status() -> dict[str, Any]:
    inv = audit_core_modules()
    return {
        "success": True,
        "engine_name": "VertictHub PyObfuscator Core",
        "engine_version": "3.0.0-android",
        "python_version": platform.python_version(),
        "platform": platform.platform(),
        "machine": platform.machine(),
        "pycryptodome_available": check_pycryptodome_available(),
        "modules_expected": inv["total_expected"],
        "modules_present": inv["modules_present"],
        "modules_loaded": inv["modules_loaded"],
        "modules_verified": inv["modules_verified"],
        "build_modes": list(BUILD_MODES.values()),
    }


def get_core_inventory() -> dict[str, Any]:
    return audit_core_modules()


def compute_ssl_pin(cert_pem_or_der_b64: str) -> dict[str, Any]:
    try:
        pin = compute_spki_pin_from_pem_or_der(cert_pem_or_der_b64)
        return {"success": True, "pin_sha256": pin}
    except Exception as exc:
        return {"success": False, "error": f"{type(exc).__name__}: {exc}"}


def run_transparent_diagnostics(
    enable_antidebug_probe: bool = True,
    enable_antisandbox_probe: bool = False,
    enable_hook_probe: bool = False,
    antisandbox_threshold: int = 6,
) -> dict[str, Any]:
    report = collect_transparent_environment_report(
        enable_antidebug_probe=enable_antidebug_probe,
        enable_antisandbox_probe=enable_antisandbox_probe,
        enable_hook_probe=enable_hook_probe,
        antisandbox_threshold=antisandbox_threshold,
    )
    report["generators"] = verify_diagnostic_generators()
    report["success"] = True
    return report


def analyze_project_json(source: str, filename: str = "input.py") -> str:
    return json.dumps(analyze_project(source, filename=filename), ensure_ascii=False)


def process_project_json(source: str, options_json: str, filename: str = "main.py") -> str:
    opts = json.loads(options_json) if options_json else {}
    return json.dumps(process_project(source, options=opts, filename=filename), ensure_ascii=False)


def build_project_json(project_json: str, options_json: str) -> str:
    proj = json.loads(project_json) if project_json else {}
    opts = json.loads(options_json) if options_json else {}
    return json.dumps(build_project(proj, opts), ensure_ascii=False)


def get_engine_status_json() -> str:
    return json.dumps(get_engine_status(), ensure_ascii=False)


def get_core_inventory_json() -> str:
    return json.dumps(get_core_inventory(), ensure_ascii=False)


def run_transparent_diagnostics_json(options_json: str = "{}") -> str:
    opts = json.loads(options_json) if options_json else {}
    return json.dumps(
        run_transparent_diagnostics(
            enable_antidebug_probe=bool(opts.get("enable_antidebug_probe", True)),
            enable_antisandbox_probe=bool(opts.get("enable_antisandbox_probe", False)),
            enable_hook_probe=bool(opts.get("enable_hook_probe", False)),
            antisandbox_threshold=int(opts.get("antisandbox_threshold", 6)),
        ),
        ensure_ascii=False,
    )


def compute_ssl_pin_json(cert_pem_or_der_b64: str) -> str:
    return json.dumps(compute_ssl_pin(cert_pem_or_der_b64), ensure_ascii=False)
