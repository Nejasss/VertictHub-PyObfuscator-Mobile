"""
runtime_fetch.py - solves the "marshal bytecode only runs on the
exact Python minor version it was built with" problem WITHOUT forcing
every launcher to bundle a full Python interpreter (which is what
PyInstaller mode already does, and why it's heavy).

Strategy: check-first, download-only-if-needed.
  1. The build bakes in which Python version (major.minor) it was
     compiled under (e.g. (3, 12)).
  2. At launch, compare that against sys.version_info on the CURRENT
     machine.
  3. Exact match -> do nothing extra, run with the system Python that
     already launched this process. Zero download, zero size cost -
     this is the common case (most users run whatever Python version
     they built with).
  4. Mismatch -> fetch a portable, self-contained CPython build for
     the NEEDED version from python-build-standalone (an official,
     widely-used project maintained by Astral - the same org behind
     uv/ruff - specifically for this "portable, relocatable CPython"
     use case: https://github.com/astral-sh/python-build-standalone).
     Cache it under ~/.vh_runtimes/<version>-<arch>/ so this download
     only ever happens ONCE per (version, architecture) combination on
     a given machine, not once per script/run. Then re-exec the
     current process under that portable interpreter.

This keeps every build's launcher small (no bundled interpreter) while
still making version mismatches self-heal automatically instead of
just failing outright.

WHY NO HARDCODED VERSION TABLE: python-build-standalone cuts a new
release roughly monthly, bumping CPython patch versions each time
(3.12.7 -> 3.12.8 -> 3.12.9, etc) and old release assets eventually
age out / get superseded. A hardcoded {(3, 12): "3.12.8"} table goes
stale the moment upstream ships a new release, silently 404ing on old
pinned URLs. Instead, this module resolves everything DYNAMICALLY at
runtime, using two pieces of upstream's own machine-readable API,
specifically documented for this purpose (see
https://github.com/astral-sh/python-build-standalone/blob/main/docs/running.rst):
  - latest-release.json: a small, stable-URL JSON file always pointing
    at the current latest release tag - avoids needing to hardcode ANY
    tag.
  - GitHub's release API (api.github.com/repos/.../releases/tags/<tag>):
    lists every asset in that release; this module filters for the
    one matching the wanted (major, minor, arch) via regex on the
    filename, rather than needing to know the exact patch version in
    advance.
This means the module keeps working indefinitely without edits, as
long as python-build-standalone keeps publishing releases in the same
format they've used since the project's early history.

Platform coverage: python-build-standalone publishes builds for many
(platform, arch) combinations; this module only wires up the two
requested here - linux x86_64 (desktop) and linux aarch64 (Termux/
Android arm64, which reports as "aarch64" via platform.machine() the
same as regular ARM64 Linux). Windows/macOS are NOT wired up (no
target for those was requested) - _arch_triple raises a clear
RuntimeError if run on an unrecognized platform rather than silently
guessing.

KNOWN LIMITATIONS (stated plainly, not hidden):
  - Requires internet access the FIRST time a given (version, arch)
    combo is needed on a machine (both to resolve the latest tag/asset
    list, AND to download the ~20-40MB archive itself). If there's no
    network and no cached runtime yet, this fails with a clear message -
    there is no way around this being necessary at least once.
  - Unauthenticated GitHub API calls are rate-limited to 60/hour per
    IP. This module only makes ONE such call per (version, arch)
    combo NOT already cached, so this is unlikely to matter in normal
    use, but is worth knowing if testing this repeatedly in a short
    window from the same IP.
  - Termux specifically: ~/.vh_runtimes resolves under Termux's own
    $HOME (usually /data/data/com.termux/files/home), which is normal
    and writable - no special-casing needed there.
  - python-build-standalone's Linux builds are typically built against
    a specific glibc baseline; on a MUSL-based or very old glibc
    system they may not run - this is upstream's constraint, not
    something this module can work around.
"""
import json
import os
import platform
import stat
import tarfile
import urllib.request

LATEST_RELEASE_JSON_URL = (
    "https://raw.githubusercontent.com/astral-sh/python-build-standalone/"
    "latest-release/latest-release.json"
)
GITHUB_API_RELEASE_BY_TAG = (
    "https://api.github.com/repos/astral-sh/python-build-standalone/releases/tags/{tag}"
)

CACHE_ROOT = os.path.join(os.path.expanduser("~"), ".vh_runtimes")
_UA_HEADERS = {"User-Agent": "vertcithub-pyobfuscator", "Accept": "application/vnd.github+json"}


class RuntimeFetchError(RuntimeError):
    pass


def _arch_triple() -> str:
    """
    Maps the current machine's (system, arch) to the LLVM target
    triple python-build-standalone uses in its asset filenames. Only
    linux x86_64 and linux aarch64 are wired up - see module docstring.
    """
    system = platform.system().lower()
    machine = platform.machine().lower()

    if system != "linux":
        raise RuntimeFetchError(
            f"runtime_fetch.py cuma didukung di Linux/Termux saat ini "
            f"(kedetek: {platform.system()}). Windows/macOS belum di-wire."
        )
    if machine in ("x86_64", "amd64"):
        return "x86_64-unknown-linux-gnu"
    if machine in ("aarch64", "arm64"):
        return "aarch64-unknown-linux-gnu"
    raise RuntimeFetchError(
        f"Arsitektur '{machine}' belum didukung runtime_fetch.py "
        f"(cuma x86_64 dan aarch64/arm64 linux)."
    )


def _fetch_json(url: str) -> dict:
    req = urllib.request.Request(url, headers=_UA_HEADERS)
    with urllib.request.urlopen(req, timeout=20) as resp:
        return json.loads(resp.read().decode("utf-8"))


def _resolve_download_url(major: int, minor: int) -> tuple:
    """
    Returns (asset_filename, download_url) for the newest available
    python-build-standalone install_only build matching (major, minor)
    on the current architecture, resolved dynamically against
    upstream's latest release - no hardcoded tag or patch version.
    """
    try:
        latest = _fetch_json(LATEST_RELEASE_JSON_URL)
        tag = latest["tag"]
    except Exception as e:
        raise RuntimeFetchError(
            f"Gagal ambil info rilis terbaru python-build-standalone: {e}. "
            f"Cek koneksi internet."
        )

    try:
        release = _fetch_json(GITHUB_API_RELEASE_BY_TAG.format(tag=tag))
        assets = release.get("assets", [])
    except Exception as e:
        raise RuntimeFetchError(
            f"Gagal ambil daftar file rilis {tag} dari GitHub API: {e}. "
            f"Kalau ini kejadian berkali-kali dalam waktu deket, mungkin "
            f"kena rate limit GitHub API (60 request/jam tanpa auth)."
        )

    arch = _arch_triple()
    # Match e.g. "cpython-3.12.8+20260718-x86_64-unknown-linux-gnu-install_only.tar.gz"
    # - deliberately NOT anchoring the patch version, since that's
    # exactly the piece that changes release to release.
    prefix = f"cpython-{major}.{minor}."
    suffix = f"-{arch}-install_only.tar.gz"
    matches = [
        a for a in assets
        if a.get("name", "").startswith(prefix) and a.get("name", "").endswith(suffix)
    ]
    if not matches:
        raise RuntimeFetchError(
            f"Gak nemu build Python {major}.{minor} untuk {arch} di rilis "
            f"python-build-standalone {tag}. Mungkin versi {major}.{minor} "
            f"udah gak dipublish lagi (misal udah EOL), atau arsitektur ini "
            f"emang gak disediakan."
        )
    # If somehow more than one matches (shouldn't normally happen for a
    # single release), take the shortest name - the plain
    # "...-install_only.tar.gz" rather than any variant with extra
    # suffixes that might also happen to satisfy the pattern.
    matches.sort(key=lambda a: len(a["name"]))
    asset = matches[0]
    return asset["name"], asset["browser_download_url"]


def _cache_dir_for(major: int, minor: int) -> str:
    system = platform.system().lower()
    machine = platform.machine().lower()
    return os.path.join(CACHE_ROOT, f"{major}.{minor}-{system}-{machine}")


def find_cached_python(major: int, minor: int) -> str:
    """
    Returns the path to python3 inside an already-downloaded+extracted
    portable runtime for (major, minor), or None if not cached yet.
    """
    cache_dir = _cache_dir_for(major, minor)
    candidate = os.path.join(cache_dir, "python", "bin", "python3")
    if os.path.isfile(candidate) and os.access(candidate, os.X_OK):
        return candidate
    return None


def download_and_cache_python(major: int, minor: int, progress_cb=None) -> str:
    """
    Resolves + downloads + extracts a portable CPython (major.minor)
    build from python-build-standalone's current latest release into
    ~/.vh_runtimes/<version>-<platform>/, then returns the path to its
    python3 executable.

    progress_cb, if given, is called with (bytes_downloaded, total_bytes
    or None) periodically during download - purely for a CLI progress
    indicator, has no effect on behavior.

    Raises RuntimeFetchError on any failure (no network, bad asset,
    extraction failure, etc.) with a message meant to be shown directly
    to the end user, not just logged.
    """
    already = find_cached_python(major, minor)
    if already:
        return already

    asset_name, url = _resolve_download_url(major, minor)
    cache_dir = _cache_dir_for(major, minor)
    os.makedirs(cache_dir, exist_ok=True)
    archive_path = os.path.join(cache_dir, asset_name)

    try:
        req = urllib.request.Request(url, headers=_UA_HEADERS)
        with urllib.request.urlopen(req, timeout=30) as resp:
            total = resp.headers.get("Content-Length")
            total = int(total) if total else None
            downloaded = 0
            with open(archive_path, "wb") as f:
                while True:
                    chunk = resp.read(1024 * 256)
                    if not chunk:
                        break
                    f.write(chunk)
                    downloaded += len(chunk)
                    if progress_cb:
                        progress_cb(downloaded, total)
    except Exception as e:
        raise RuntimeFetchError(
            f"Gagal download Python portable {major}.{minor} dari {url}: {e}. "
            f"Cek koneksi internet."
        )

    try:
        with tarfile.open(archive_path, "r:gz") as tf:
            # python-build-standalone's *-install_only.tar.gz archives
            # always extract to a top-level "python/" directory.
            try:
                tf.extractall(cache_dir, filter="data")
            except TypeError:
                # Python < 3.12 doesn't have the filter= kwarg yet.
                tf.extractall(cache_dir)
    except Exception as e:
        raise RuntimeFetchError(f"Gagal extract {archive_path}: {e}")
    finally:
        try:
            os.remove(archive_path)
        except OSError:
            pass

    py_bin = os.path.join(cache_dir, "python", "bin", "python3")
    if not os.path.isfile(py_bin):
        raise RuntimeFetchError(
            f"Extract selesai tapi python3 gak ketemu di {py_bin} - "
            f"struktur archive mungkin berubah di upstream."
        )
    st = os.stat(py_bin)
    os.chmod(py_bin, st.st_mode | stat.S_IEXEC | stat.S_IXGRP | stat.S_IXOTH)
    return py_bin


def ensure_python(major: int, minor: int, progress_cb=None) -> str:
    """
    Convenience wrapper: returns a working python3 path for
    (major, minor), using the cache if present, downloading otherwise.
    """
    cached = find_cached_python(major, minor)
    if cached:
        return cached
    return download_and_cache_python(major, minor, progress_cb=progress_cb)

