"""Verification script asserting all 34 original vh/core/*.py files are present and importable."""
from __future__ import annotations

import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(ROOT / "app/src/main/python"))

from vh.android_adapter.compatibility import MANDATORY_CORE_MODULES, audit_core_modules  # noqa: E402


def main() -> int:
    report = audit_core_modules()
    assert report["total_expected"] == 34, f"Expected 34 modules, got {report['total_expected']}"
    assert report["modules_present"] == 34, f"Missing files: {34 - report['modules_present']}"
    assert report["modules_loaded"] == 34, f"Failed imports: {34 - report['modules_loaded']}"

    sha_verified = sum(1 for m in report["modules"] if m["sha256_verified"])
    assert sha_verified == 34, f"Expected 34 SHA-256 matches against core_manifest.json, got {sha_verified}"

    print(
        json.dumps(
            {
                "status": "PASS",
                "total_expected": len(MANDATORY_CORE_MODULES),
                "modules_present": report["modules_present"],
                "modules_loaded": report["modules_loaded"],
                "modules_verified": report["modules_verified"],
                "sha256_verified": sha_verified,
                "pycryptodome_available": report["pycryptodome_available"],
            },
            indent=2,
        )
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
