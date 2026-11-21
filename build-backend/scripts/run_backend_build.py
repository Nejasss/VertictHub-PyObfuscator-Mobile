"""Headless host build runner bridging build-backend requests to original_build.py / core toolchains."""
from __future__ import annotations

import argparse
import hashlib
import json
import subprocess
import sys
from pathlib import Path


def main() -> int:
    parser = argparse.ArgumentParser(description="VertictHub Build-Backend Runner")
    parser.add_argument("--input", required=True, help="Path to input .py script")
    parser.add_argument("--mode", default="pyinstaller", choices=["protected_python", "pyinstaller", "cython_native", "embedded_build", "native_launcher", "rust_launcher"])
    parser.add_argument("--target", default="linux-x86_64")
    parser.add_argument("--output-dir", default="artifacts")
    parser.add_argument("--output-name", default="app_protected_bin")
    parser.add_argument("--onefile", default="true")
    parser.add_argument("--console", default="true")
    parser.add_argument("--cython-guard", default="true")
    parser.add_argument("--run-leak-scan", default="true")
    args = parser.parse_args()

    repo_root = Path(__file__).resolve().parents[2]
    sys.path.insert(0, str(repo_root / "app/src/main/python"))

    from vh.android_adapter.api import process_project
    from vh.core import leak_scan

    input_path = Path(args.input).resolve()
    source = input_path.read_text(encoding="utf-8")
    out_dir = Path(args.output_dir).resolve()
    out_dir.mkdir(parents=True, exist_ok=True)

    stage1 = process_project(
        source,
        options={
            "output_mode": "cython_source" if args.mode in ("cython_native", "embedded_build") else "py_launcher",
            "output_filename": f"{args.output_name}.py",
            "run_leak_scan": True,
        },
        filename=input_path.name,
    )
    if not stage1.get("success"):
        print(json.dumps(stage1, indent=2), file=sys.stderr)
        return 1

    stage1_file = out_dir / f"{args.output_name}_stage1.py"
    stage1_file.write_text(stage1["output"]["source"], encoding="utf-8")

    artifact_path = stage1_file
    if args.mode != "protected_python":
        orig_build = repo_root / "build-backend/scripts/original_build.py"
        cmd = [sys.executable, str(orig_build), "--input", str(input_path), "--output-dir", str(out_dir), "--name", args.output_name]
        if args.mode == "cython_native":
            cmd.append("--cython")
        elif args.mode == "embedded_build":
            cmd.append("--embed")
        elif args.mode == "native_launcher":
            cmd.append("--native-launcher")
        elif args.mode == "rust_launcher":
            cmd.append("--rust-launcher")
        if args.onefile.lower() == "true":
            cmd.append("--onefile")
        if args.console.lower() != "true":
            cmd.append("--noconsole")
        subprocess.run(cmd, check=True)
        candidates = [p for p in out_dir.iterdir() if p.is_file() and p.name != stage1_file.name]
        if candidates:
            artifact_path = max(candidates, key=lambda p: p.stat().st_mtime)

    vocab = leak_scan.extract_sensitive_vocabulary(source)
    scan_res = leak_scan.scan_binary_for_leaks(str(artifact_path), vocab)

    raw_bytes = artifact_path.read_bytes()
    manifest = {
        "mode": args.mode,
        "target": args.target,
        "artifact": artifact_path.name,
        "size_bytes": len(raw_bytes),
        "sha256": hashlib.sha256(raw_bytes).hexdigest(),
        "leaked_names_count": len(scan_res.get("leaked_names", [])),
        "leaked_strings_count": len(scan_res.get("leaked_strings", [])),
    }
    (out_dir / "build_manifest.json").write_text(json.dumps(manifest, indent=2), encoding="utf-8")
    print(json.dumps(manifest, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
