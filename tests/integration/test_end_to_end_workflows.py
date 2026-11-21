import json
import unittest
from pathlib import Path

from vh.android_adapter.api import (
    analyze_project_json,
    build_project_json,
    get_core_inventory_json,
    get_engine_status_json,
    process_project_json,
)

SAMPLE_PATH = Path(__file__).resolve().parents[1] / "fixtures/sample_app.py"


class EndToEndWorkflowTests(unittest.TestCase):
    def test_json_bridge_contract(self):
        source = SAMPLE_PATH.read_text(encoding="utf-8")
        status = json.loads(get_engine_status_json())
        self.assertTrue(status["success"])
        self.assertEqual(status["modules_verified"], 34)

        inv = json.loads(get_core_inventory_json())
        self.assertEqual(inv["modules_loaded"], 34)

        analysis = json.loads(analyze_project_json(source, "sample_app.py"))
        self.assertTrue(analysis["success"])

        processed = json.loads(
            process_project_json(
                source,
                json.dumps({"output_mode": "cython_source", "preset": "cython_ready"}),
                "sample_app.py",
            )
        )
        self.assertTrue(processed["success"])
        self.assertIn("output", processed)

        built = json.loads(
            build_project_json(
                json.dumps({"filename": "sample_app.py", "source": source}),
                json.dumps({"build_mode": "rust_launcher", "target_platform": "linux-x86_64"}),
            )
        )
        self.assertTrue(built["success"])
        self.assertEqual(built["workflow_file"], ".github/workflows/core-build.yml")
