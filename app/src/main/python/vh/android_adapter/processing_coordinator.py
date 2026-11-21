"""Coordinates on-device source analysis, AST validation, obfuscation pipeline, and leak scanning."""
from __future__ import annotations

import ast
import hashlib
import os
import tempfile
import time
from typing import Any, Callable

from vh.core import ast_integrity, constant_protection, leak_scan, obfuscator
from .compatibility import audit_core_modules, check_pycryptodome_available
from .configuration import AndroidTransformOptions
from .exceptions import AdapterError
from .result_mapper import extract_transformation_summaries


def analyze_python_source(source: str, filename: str = "input.py") -> dict[str, Any]:
    if not source or not source.strip():
        raise AdapterError("EMPTY_SOURCE", "File Python kosong.", stage="input_analysis")

    try:
        ast_report = ast_integrity.validate_source(source, stage="workspace-analysis", filename=filename)
        docstring_contract = ast_integrity.capture_docstring_contract(source, stage="workspace-analysis")
    except ast_integrity.ASTIntegrityError as exc:
        raise AdapterError("SYNTAX_ERROR", str(exc), stage="workspace-analysis") from exc

    tree = ast.parse(source, filename=filename)
    imports: list[str] = []
    for node in ast.walk(tree):
        if isinstance(node, ast.Import):
            for alias in node.names:
                imports.append(alias.name)
        elif isinstance(node, ast.ImportFrom):
            prefix = "." * node.level
            mod = node.module or ""
            imports.append(f"{prefix}{mod}")

    const_inv = constant_protection.scan_constants(source)
    vocab = leak_scan.extract_sensitive_vocabulary(source)

    lines = source.splitlines()
    return {
        "success": True,
        "filename": filename,
        "size_bytes": len(source.encode("utf-8")),
        "line_count": len(lines),
        "ast": {
            "valid": True,
            "node_count": ast_report.node_count,
            "function_count": ast_report.function_count,
            "async_function_count": ast_report.async_function_count,
            "class_count": ast_report.class_count,
            "import_count": ast_report.import_count,
            "future_import_count": ast_report.future_import_count,
            "module_docstring_present": docstring_contract.module_present,
            "definition_docstring_count": docstring_contract.definition_count,
        },
        "detected_imports": sorted(set(imports)),
        "constant_inventory": {
            "total_supported_literals": const_inv.total_supported_literals,
            "counts_by_type": dict(const_inv.counts_by_type),
        },
        "sensitive_vocabulary_counts": {
            "names": len(vocab.get("names", [])),
            "strings": len(vocab.get("strings", [])),
        },
    }


def execute_transformation_pipeline(
    source: str,
    raw_options: dict[str, Any] | None = None,
    filename: str = "main.py",
    progress_cb: Callable[[str, str, int], None] | None = None,
) -> dict[str, Any]:
    started_at = time.monotonic()

    def emit(stage: str, module: str, pct: int) -> None:
        if progress_cb is not None:
            progress_cb(stage, module, pct)

    emit("Auditing Core Modules", "compatibility.py", 5)
    inventory = audit_core_modules()

    if not source or not source.strip():
        raise AdapterError("EMPTY_SOURCE", "Source Python tidak boleh kosong.", stage="input")

    opts = AndroidTransformOptions.from_dict(raw_options)
    out_name = opts.output_filename.strip() or f"{os.path.splitext(filename)[0]}_protected.py"

    emit("Validating Input AST", "ast_integrity.py", 15)
    try:
        input_ast = ast_integrity.validate_source(source, stage="input", filename=filename)
        input_docs = ast_integrity.capture_docstring_contract(source, stage="input")
    except ast_integrity.ASTIntegrityError as exc:
        raise AdapterError("INPUT_AST_INVALID", str(exc), stage="input") from exc

    emit("Extracting Sensitive Vocabulary", "leak_scan.py", 25)
    vocab = leak_scan.extract_sensitive_vocabulary(source)

    core_cfg = opts.to_core_config()
    gate_info = obfuscator.generate_gate_info() if opts.require_launcher_gate else None

    emit("Running Core Obfuscation Pipeline", "obfuscator.py", 55)
    logs: list[str] = [
        f"[engine] Input: {filename} ({len(source.encode('utf-8'))} bytes, {len(source.splitlines())} lines)",
        f"[engine] Mode: {opts.output_mode} | Preset: {opts.preset}",
        f"[engine] Launcher gate: {'ENABLED (ephemeral build token)' if gate_info else 'DISABLED'}",
    ]

    try:
        if opts.output_mode == "cython_source":
            transformed = obfuscator.obfuscate_source_for_cython(source, core_cfg, gate_info=gate_info)
        else:
            if not check_pycryptodome_available():
                raise AdapterError(
                    "MISSING_PYCRYPTODOME",
                    "PyCryptodome diperlukan untuk packer AES-256-GCM pada mode py_launcher.",
                    stage="packer",
                )
            transformed = obfuscator.obfuscate_source(
                source,
                core_cfg,
                expected_filename=os.path.basename(out_name),
                gate_info=gate_info,
            )
    except ast_integrity.ASTIntegrityError as exc:
        raise AdapterError("AST_INTEGRITY_FAILED", str(exc), stage="transform") from exc
    except ValueError as exc:
        raise AdapterError("CONFIGURATION_ERROR", str(exc), stage="transform") from exc
    except Exception as exc:
        raise AdapterError("TRANSFORM_FAILED", f"{type(exc).__name__}: {exc}", stage="transform") from exc

    emit("Validating Output AST", "ast_integrity.py", 75)
    try:
        output_ast = ast_integrity.validate_source(transformed, stage="output", filename=out_name)
    except ast_integrity.ASTIntegrityError as exc:
        raise AdapterError("OUTPUT_AST_INVALID", str(exc), stage="output_validation") from exc

    emit("Scanning Output for Identifier/String Leaks", "leak_scan.py", 88)
    leak_result: dict[str, Any] = {"skipped": True}
    if opts.run_leak_scan:
        tmp_fd, tmp_path = tempfile.mkstemp(prefix="vh_scan_", suffix=".py")
        try:
            with os.fdopen(tmp_fd, "w", encoding="utf-8") as tf:
                tf.write(transformed)
            raw_scan = leak_scan.scan_binary_for_leaks(tmp_path, vocab)
            leak_result = {
                "skipped": False,
                "leaked_names": raw_scan.get("leaked_names", []),
                "leaked_strings": raw_scan.get("leaked_strings", []),
                "total_leaks": len(raw_scan.get("leaked_names", [])) + len(raw_scan.get("leaked_strings", [])),
                "formatted_report": leak_scan.format_report(raw_scan, binary_label=out_name),
            }
        finally:
            if os.path.exists(tmp_path):
                os.remove(tmp_path)

    emit("Generating Integrity Manifest & Report", "result_mapper.py", 96)
    transformations, warnings = extract_transformation_summaries(core_cfg, opts)
    in_bytes = source.encode("utf-8")
    out_bytes = transformed.encode("utf-8")
    elapsed_ms = int((time.monotonic() - started_at) * 1000)

    logs.append(f"[ast_integrity] Output AST valid ({output_ast.node_count} nodes)")
    if not leak_result.get("skipped"):
        logs.append(f"[leak_scan] Total leaked tokens: {leak_result['total_leaks']}")
    logs.append(f"[engine] Completed in {elapsed_ms} ms")

    emit("Completed", "api.py", 100)
    return {
        "success": True,
        "status": "completed",
        "execution_environment": "ON_DEVICE",
        "input": {
            "filename": filename,
            "size": len(in_bytes),
            "sha256": hashlib.sha256(in_bytes).hexdigest(),
        },
        "output": {
            "filename": out_name,
            "size": len(out_bytes),
            "sha256": hashlib.sha256(out_bytes).hexdigest(),
            "source": transformed,
        },
        "core": {
            "modules_present": inventory["modules_present"],
            "modules_loaded": inventory["modules_loaded"],
            "modules_verified": inventory["modules_verified"],
        },
        "transformations": transformations,
        "integrity": {
            "input_sha256": hashlib.sha256(in_bytes).hexdigest(),
            "output_sha256": hashlib.sha256(out_bytes).hexdigest(),
            "launcher_gate_enabled": bool(gate_info),
            "gate_env_name": gate_info["env_name"] if gate_info else None,
            "filename_locked": opts.lock_filename,
            "leak_scan": leak_result,
        },
        "validation": {
            "input_valid": True,
            "output_valid": True,
            "input_nodes": input_ast.node_count,
            "output_nodes": output_ast.node_count,
            "input_docstrings": input_docs.definition_count + (1 if input_docs.module_present else 0),
        },
        "warnings": warnings,
        "statistics": {
            "elapsed_ms": elapsed_ms,
            "input_lines": len(source.splitlines()),
            "output_lines": len(transformed.splitlines()),
            "size_delta_bytes": len(out_bytes) - len(in_bytes),
        },
        "logs": logs,
    }
