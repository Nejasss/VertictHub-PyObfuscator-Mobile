"""Maps Android UI transformation options and presets to core.obfuscator.ObfuscationConfig."""
from __future__ import annotations

from dataclasses import dataclass, field
from typing import Any

from vh.core.obfuscator import ObfuscationConfig

PRESETS: dict[str, dict[str, Any]] = {
    "default_cli": {
        "encrypt_strings": True,
        "min_string_len": 1,
        "junk_intensity": 6,
        "anti_debug": True,
        "anti_vpn": False,
        "strict_network_check": False,
        "rename_identifiers": True,
        "lock_filename": False,
        "require_launcher_gate": True,
        "inject_decoy_methods": True,
        "periodic_recheck": True,
        "enable_version_fallback": False,
        "enable_antisandbox": False,
        "antisandbox_threshold": 6,
        "bytecode_integrity_check": False,
        "cjk_identifiers": False,
        "hook_detection": False,
        "control_flow_obfuscation": False,
        "constant_protection": False,
        "expression_obfuscation": False,
        "import_obfuscation": False,
        "attribute_obfuscation": False,
        "function_transformation": False,
        "data_flow_obfuscation": False,
    },
    "source_safe": {
        "encrypt_strings": True,
        "min_string_len": 2,
        "junk_intensity": 3,
        "anti_debug": False,
        "anti_vpn": False,
        "strict_network_check": False,
        "rename_identifiers": True,
        "lock_filename": False,
        "require_launcher_gate": True,
        "inject_decoy_methods": False,
        "periodic_recheck": False,
        "enable_version_fallback": False,
        "enable_antisandbox": False,
        "antisandbox_threshold": 6,
        "bytecode_integrity_check": False,
        "cjk_identifiers": False,
        "hook_detection": False,
        "control_flow_obfuscation": False,
        "constant_protection": True,
        "expression_obfuscation": True,
        "import_obfuscation": False,
        "attribute_obfuscation": False,
        "function_transformation": False,
        "data_flow_obfuscation": False,
    },
    "maximum_ast": {
        "encrypt_strings": True,
        "min_string_len": 1,
        "junk_intensity": 8,
        "anti_debug": True,
        "anti_vpn": False,
        "strict_network_check": False,
        "rename_identifiers": True,
        "lock_filename": True,
        "require_launcher_gate": True,
        "inject_decoy_methods": True,
        "periodic_recheck": True,
        "enable_version_fallback": False,
        "enable_antisandbox": False,
        "antisandbox_threshold": 6,
        "bytecode_integrity_check": False,
        "cjk_identifiers": True,
        "hook_detection": False,
        "control_flow_obfuscation": True,
        "constant_protection": True,
        "expression_obfuscation": True,
        "import_obfuscation": True,
        "attribute_obfuscation": True,
        "function_transformation": True,
        "data_flow_obfuscation": True,
    },
    "cython_ready": {
        "encrypt_strings": True,
        "min_string_len": 1,
        "junk_intensity": 5,
        "anti_debug": True,
        "anti_vpn": False,
        "strict_network_check": False,
        "rename_identifiers": True,
        "lock_filename": False,
        "require_launcher_gate": True,
        "inject_decoy_methods": True,
        "periodic_recheck": True,
        "enable_version_fallback": False,
        "enable_antisandbox": False,
        "antisandbox_threshold": 6,
        "bytecode_integrity_check": False,
        "cjk_identifiers": False,
        "hook_detection": False,
        "control_flow_obfuscation": False,
        "constant_protection": True,
        "expression_obfuscation": True,
        "import_obfuscation": True,
        "attribute_obfuscation": True,
        "function_transformation": True,
        "data_flow_obfuscation": True,
    },
}


@dataclass
class AndroidTransformOptions:
    preset: str = "default_cli"
    output_mode: str = "py_launcher"
    validate_input: bool = True
    validate_output: bool = True
    preserve_docstrings: bool = True
    run_leak_scan: bool = True
    output_filename: str = "protected_output.py"
    encrypt_strings: bool = True
    min_string_len: int = 1
    junk_intensity: int = 6
    anti_debug: bool = True
    anti_vpn: bool = False
    strict_network_check: bool = False
    rename_identifiers: bool = True
    lock_filename: bool = False
    require_launcher_gate: bool = True
    inject_decoy_methods: bool = True
    periodic_recheck: bool = False
    enable_version_fallback: bool = False
    enable_antisandbox: bool = False
    antisandbox_threshold: int = 6
    bytecode_integrity_check: bool = False
    recheck_interval_min: int = 4
    recheck_interval_max: int = 9
    cjk_identifiers: bool = False
    ssl_pin_sha256: list[str] = field(default_factory=list)
    hook_detection: bool = False
    control_flow_obfuscation: bool = False
    constant_protection: bool = False
    expression_obfuscation: bool = False
    import_obfuscation: bool = False
    attribute_obfuscation: bool = False
    function_transformation: bool = False
    data_flow_obfuscation: bool = False

    @classmethod
    def from_dict(cls, raw: dict[str, Any] | None) -> "AndroidTransformOptions":
        data = dict(raw or {})
        preset_name = str(data.get("preset", "default_cli"))
        merged: dict[str, Any] = {}
        if preset_name in PRESETS and data.get("apply_preset_defaults", False):
            merged.update(PRESETS[preset_name])
        merged.update(data)

        pins_raw = merged.get("ssl_pin_sha256") or []
        if isinstance(pins_raw, str):
            pins = [p.strip().lower() for p in pins_raw.split(",") if p.strip()]
        else:
            pins = [str(p).strip().lower() for p in pins_raw if str(p).strip()]

        min_iv = max(1, int(merged.get("recheck_interval_min", 4)))
        max_iv = max(min_iv, int(merged.get("recheck_interval_max", 9)))
        output_mode = str(merged.get("output_mode", "py_launcher"))
        req_gate = True if output_mode == "py_launcher" else bool(merged.get("require_launcher_gate", True))

        return cls(
            preset=preset_name,
            output_mode=output_mode,
            validate_input=bool(merged.get("validate_input", True)),
            validate_output=bool(merged.get("validate_output", True)),
            preserve_docstrings=bool(merged.get("preserve_docstrings", True)),
            run_leak_scan=bool(merged.get("run_leak_scan", True)),
            output_filename=str(merged.get("output_filename", "protected_output.py")),
            encrypt_strings=bool(merged.get("encrypt_strings", True)),
            min_string_len=max(1, int(merged.get("min_string_len", 1))),
            junk_intensity=max(0, min(20, int(merged.get("junk_intensity", 6)))),
            anti_debug=bool(merged.get("anti_debug", True)),
            anti_vpn=bool(merged.get("anti_vpn", False)),
            strict_network_check=bool(merged.get("strict_network_check", False)),
            rename_identifiers=bool(merged.get("rename_identifiers", True)),
            lock_filename=bool(merged.get("lock_filename", False)),
            require_launcher_gate=req_gate,
            inject_decoy_methods=bool(merged.get("inject_decoy_methods", True)),
            periodic_recheck=bool(merged.get("periodic_recheck", False)),
            enable_version_fallback=False,
            enable_antisandbox=bool(merged.get("enable_antisandbox", False)),
            antisandbox_threshold=max(1, int(merged.get("antisandbox_threshold", 6))),
            bytecode_integrity_check=bool(merged.get("bytecode_integrity_check", False)),
            recheck_interval_min=min_iv,
            recheck_interval_max=max_iv,
            cjk_identifiers=bool(merged.get("cjk_identifiers", False)),
            ssl_pin_sha256=pins,
            hook_detection=bool(merged.get("hook_detection", False)),
            control_flow_obfuscation=bool(merged.get("control_flow_obfuscation", False)),
            constant_protection=bool(merged.get("constant_protection", False)),
            expression_obfuscation=bool(merged.get("expression_obfuscation", False)),
            import_obfuscation=bool(merged.get("import_obfuscation", False)),
            attribute_obfuscation=bool(merged.get("attribute_obfuscation", False)),
            function_transformation=bool(merged.get("function_transformation", False)),
            data_flow_obfuscation=bool(merged.get("data_flow_obfuscation", False)),
        )

    def to_core_config(self) -> ObfuscationConfig:
        return ObfuscationConfig(
            encrypt_strings=self.encrypt_strings,
            min_string_len=self.min_string_len,
            junk_intensity=self.junk_intensity,
            anti_debug=self.anti_debug,
            anti_vpn=self.anti_vpn,
            strict_network_check=self.strict_network_check,
            rename_identifiers=self.rename_identifiers,
            lock_filename=self.lock_filename,
            require_launcher_gate=self.require_launcher_gate,
            inject_decoy_methods=self.inject_decoy_methods,
            periodic_recheck=self.periodic_recheck,
            enable_version_fallback=self.enable_version_fallback,
            enable_antisandbox=self.enable_antisandbox,
            antisandbox_threshold=self.antisandbox_threshold,
            bytecode_integrity_check=self.bytecode_integrity_check,
            recheck_interval_range=(self.recheck_interval_min, self.recheck_interval_max),
            cjk_identifiers=self.cjk_identifiers,
            ssl_pin_sha256=list(self.ssl_pin_sha256),
            hook_detection=self.hook_detection,
            control_flow_obfuscation=self.control_flow_obfuscation,
            constant_protection=self.constant_protection,
            expression_obfuscation=self.expression_obfuscation,
            import_obfuscation=self.import_obfuscation,
            attribute_obfuscation=self.attribute_obfuscation,
            function_transformation=self.function_transformation,
            data_flow_obfuscation=self.data_flow_obfuscation,
        )
