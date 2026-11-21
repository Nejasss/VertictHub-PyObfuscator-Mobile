"""Maps core transformation reports, AST integrity reports, and leak scan results to JSON-safe dicts."""
from __future__ import annotations

from dataclasses import asdict, is_dataclass
from typing import Any


def _serialize_report(obj: Any) -> dict[str, Any] | None:
    if obj is None:
        return None
    if is_dataclass(obj):
        raw = asdict(obj)
        for k, v in list(raw.items()):
            if isinstance(v, set):
                raw[k] = sorted(v)
        return raw
    if isinstance(obj, dict):
        return dict(obj)
    return {"summary": str(obj)}


def extract_transformation_summaries(core_config: Any, opts: Any) -> tuple[list[dict[str, Any]], list[str]]:
    transformations: list[dict[str, Any]] = []
    warnings: list[str] = []

    def add_pass(key: str, label: str, enabled: bool, report_attr: str | None = None, note: str = "") -> None:
        rep = _serialize_report(getattr(core_config, report_attr, None)) if report_attr else None
        status = "applied" if enabled else "skipped_disabled"
        transformations.append({
            "key": key,
            "label": label,
            "enabled": enabled,
            "status": status,
            "report": rep,
            "note": note,
        })

    add_pass("native_worker_compat", "Runtime Worker Entrypoint Compatibility", True, "native_worker_compat_report")
    add_pass("worker_gate_compat", "Worker Gate Forwarding Compatibility", opts.require_launcher_gate, "worker_gate_forwarding_report")
    add_pass("expression_obfuscation", "Expression Obfuscation", opts.expression_obfuscation, "expression_obfuscation_report")
    add_pass("control_flow_obfuscation", "Control Flow Flattening", opts.control_flow_obfuscation, None, "Conservative single-if function flattening" if opts.control_flow_obfuscation else "Default OFF")
    add_pass("constant_protection", "Constant Protection", opts.constant_protection, "constant_protection_report")
    add_pass("import_obfuscation", "Import Obfuscation", opts.import_obfuscation, "import_obfuscation_report")
    add_pass("attribute_obfuscation", "Attribute Obfuscation", opts.attribute_obfuscation, "attribute_obfuscation_report")
    add_pass("function_transformation", "Function Prefix Transformation", opts.function_transformation, "function_transformation_report")
    add_pass("data_flow_obfuscation", "Data Flow Obfuscation", opts.data_flow_obfuscation, "data_flow_obfuscation_report")
    add_pass("inject_decoy_methods", "Decoy Method Injection", opts.inject_decoy_methods, None, "Injects 1-3 decoy methods per local class")
    add_pass("rename_identifiers", "Identifier Renaming", opts.rename_identifiers, None, "Mode: CJK (Han)" if opts.cjk_identifiers else "Mode: ASCII")
    add_pass("encrypt_strings", "String Literal & F-String Encryption", opts.encrypt_strings, None, f"min_string_len={opts.min_string_len}")
    add_pass("junk_code", "Junk Code Injection", opts.junk_intensity > 0, None, f"intensity={opts.junk_intensity}")

    if opts.anti_vpn:
        warnings.append("Anti-VPN enabled: generated output includes explicit network lookup checks when executed on target.")
    if opts.enable_antisandbox:
        warnings.append(f"Anti-Sandbox heuristic enabled (threshold={opts.antisandbox_threshold}): may trigger on low-RAM/low-core devices.")

    return transformations, warnings
