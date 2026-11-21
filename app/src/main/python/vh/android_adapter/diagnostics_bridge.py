"""Transparent diagnostic wrapper around antidebug, antisandbox, hook_detection, and ssl_pinning."""
from __future__ import annotations

import base64
import os
import platform
import sys
from typing import Any

from vh.core import antidebug, antisandbox, hook_detection, ssl_pinning


def collect_transparent_environment_report(
    *,
    enable_antidebug_probe: bool = True,
    enable_antisandbox_probe: bool = False,
    enable_hook_probe: bool = False,
    antisandbox_threshold: int = 6,
) -> dict[str, Any]:
    findings: list[dict[str, Any]] = []

    trace_active = sys.gettrace() is not None
    tracer_pid = 0
    proc_status_path = "/proc/self/status"
    if os.path.isfile(proc_status_path):
        try:
            with open(proc_status_path, "r", encoding="utf-8", errors="replace") as f:
                for line in f:
                    if line.startswith("TracerPid:"):
                        tracer_pid = int(line.split(":", 1)[1].strip() or "0")
                        break
        except OSError:
            tracer_pid = -1

    if enable_antidebug_probe:
        findings.append({
            "module": "antidebug.py",
            "check": "python_sys_gettrace",
            "triggered": trace_active,
            "detail": f"sys.gettrace()={'active' if trace_active else 'none'}",
        })
        findings.append({
            "module": "antidebug.py",
            "check": "proc_self_tracer_pid",
            "triggered": tracer_pid > 0,
            "detail": f"TracerPid={tracer_pid}",
        })

    if enable_antisandbox_probe:
        cpu_count = os.cpu_count() or 0
        score = 0
        reasons: list[str] = []
        if 0 < cpu_count <= 2:
            score += 2
            reasons.append(f"low_cpu_count={cpu_count}")
        findings.append({
            "module": "antisandbox.py",
            "check": "hardware_heuristic_score",
            "triggered": score >= antisandbox_threshold,
            "detail": f"score={score}/{antisandbox_threshold}; signals={reasons or ['none']}",
        })

    if enable_hook_probe:
        patched_targets: list[str] = []
        for mod_name, cls_name, attr_name in [
            ("requests.sessions", "Session", "send"),
            ("urllib3.connection", "HTTPSConnection", "connect"),
            ("http.client", "HTTPSConnection", "connect"),
        ]:
            try:
                mod = __import__(mod_name, fromlist=[cls_name])
                cls = getattr(mod, cls_name, None)
                fn = getattr(cls, attr_name, None) if cls else None
                fn_mod = getattr(fn, "__module__", "") if fn else ""
                if fn is not None and fn_mod and not fn_mod.startswith(mod_name.split(".")[0]):
                    patched_targets.append(f"{mod_name}.{cls_name}.{attr_name}->{fn_mod}")
            except ImportError:
                pass
        findings.append({
            "module": "hook_detection.py",
            "check": "http_client_method_origins",
            "triggered": len(patched_targets) > 0,
            "detail": f"external_patches={patched_targets or ['none']}",
        })

    return {
        "mode": "TRANSPARENT_DIAGNOSTIC_ONLY",
        "network_requests_performed": False,
        "process_termination_disabled": True,
        "platform": platform.platform(),
        "machine": platform.machine(),
        "python_version": platform.python_version(),
        "findings": findings,
    }


def compute_spki_pin_from_pem_or_der(cert_text_or_b64: str) -> str:
    cleaned = cert_text_or_b64.strip()
    if "-----BEGIN CERTIFICATE-----" in cleaned:
        lines = [
            ln.strip()
            for ln in cleaned.splitlines()
            if ln.strip() and not ln.strip().startswith("-----")
        ]
        der_bytes = base64.b64decode("".join(lines))
    else:
        der_bytes = base64.b64decode(cleaned)
    return ssl_pinning.compute_pin_from_der(der_bytes)


def verify_diagnostic_generators() -> dict[str, bool]:
    ad_pre, ad_post = antidebug.get_antidebug_source(enabled=True, anti_vpn=False)
    as_src = antisandbox.get_antisandbox_source(enabled=True, score_threshold=6)
    hd_src = hook_detection.get_hook_detection_source(enabled=True)
    ssl_src = ssl_pinning.get_ssl_pinning_source(["a" * 64], enabled=True)
    for snippet in (ad_pre, ad_post, as_src, hd_src, ssl_src):
        if snippet:
            compile(snippet, "<diagnostic-preview>", "exec")
    return {
        "antidebug_generator_ok": bool(ad_pre),
        "antisandbox_generator_ok": bool(as_src),
        "hook_detection_generator_ok": bool(hd_src),
        "ssl_pinning_generator_ok": bool(ssl_src),
    }
