import os
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

from vh.android_adapter.api import (
    analyze_project,
    build_project,
    get_engine_status,
    process_project,
    run_transparent_diagnostics,
    validate_output,
)

SAMPLE_PATH = Path(__file__).resolve().parents[1] / "fixtures/sample_app.py"


class AdapterPipelineTests(unittest.TestCase):
    def setUp(self):
        self.sample_source = SAMPLE_PATH.read_text(encoding="utf-8")

    def test_analyze_project(self):
        res = analyze_project(self.sample_source, filename="sample_app.py")
        self.assertTrue(res["success"])
        self.assertEqual(res["ast"]["class_count"], 1)
        self.assertGreaterEqual(res["ast"]["function_count"], 3)

    def test_cython_source_mode_roundtrip(self):
        res = process_project(
            self.sample_source,
            options={
                "output_mode": "cython_source",
                "output_filename": "out_cy.py",
                "encrypt_strings": True,
                "rename_identifiers": True,
                "constant_protection": True,
                "expression_obfuscation": True,
                "run_leak_scan": True,
            },
            filename="sample_app.py",
        )
        self.assertTrue(res["success"], res.get("error"))
        self.assertTrue(res["validation"]["output_valid"])
        val = validate_output(res["output"]["source"], filename="out_cy.py")
        self.assertTrue(val["valid"])

    def test_py_launcher_mode_when_pycryptodome_installed(self):
        status = get_engine_status()
        if not status["pycryptodome_available"]:
            self.skipTest("PyCryptodome not installed in test environment")

        res = process_project(
            self.sample_source,
            options={"output_mode": "py_launcher", "output_filename": "launcher_out.py"},
            filename="sample_app.py",
        )
        self.assertTrue(res["success"], res.get("error"))
        gate_env = res["integrity"]["gate_env_name"]
        self.assertTrue(gate_env)

        with tempfile.TemporaryDirectory() as td:
            out_py = Path(td) / "launcher_out.py"
            out_py.write_text(res["output"]["source"], encoding="utf-8")
            env = dict(os.environ)
            env[gate_env] = "1"
            proc = subprocess.run([sys.executable, str(out_py)], env=env, capture_output=True, text=True, timeout=15)
            self.assertEqual(proc.returncode, 0, proc.stderr)
            self.assertIn("UNIT:55:", proc.stdout)

    def test_build_backend_dispatch_payload(self):
        res = build_project(
            {"filename": "sample_app.py", "source": self.sample_source},
            {"build_mode": "cython_native", "target_platform": "android-arm64-ndk", "output_filename": "demo_bin"},
        )
        self.assertTrue(res["success"])
        self.assertTrue(res["requires_github_actions"])
        self.assertEqual(res["dispatch_payload"]["inputs"]["build_mode"], "cython_native")

    def test_transparent_diagnostics_safe_mode(self):
        rep = run_transparent_diagnostics(enable_antidebug_probe=True, enable_antisandbox_probe=True, enable_hook_probe=True)
        self.assertTrue(rep["success"])
        self.assertFalse(rep["network_requests_performed"])
        self.assertTrue(rep["process_termination_disabled"])
