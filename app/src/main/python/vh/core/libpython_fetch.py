#!/usr/bin/env python3
"""
libpython_fetch.py - companion to runtime_fetch.py, but narrower in
scope: instead of fetching/caching a FULL portable Python interpreter
(what mode 1's version-fallback needs), this fetches/caches just the
libpythonX.Y shared library a Cython/embed binary needs to dynamically
link against at process-start time.

Why this is a separate script from runtime_fetch.py rather than a
function inside it: this is invoked over subprocess by COMPILED C
code (native_launcher.py's ensure_libpython_version feature), which
needs a single, simple contract - "run this script with two args
(major minor), read one line of stdout: either a directory path (on
success) or nothing (on failure, check stderr for why) - not
Python-level function calls, since the C caller isn't a Python
process itself at that point.

Reuses the EXACT SAME cache root (~/.vh_runtimes/) and
directory-naming scheme as runtime_fetch.py, so a machine that already
downloaded a portable Python for mode 1's version-fallback already has
the .so this needs too (it's inside that same install_only archive),
and vice versa - no duplicate downloads across the two features.

Usage: python3 libpython_fetch.py <major> <minor>
Prints (and exits 0): the directory containing libpythonX.Y.so on
    success - either because the full portable runtime was already
    cached (from either this script or runtime_fetch.py having run
    before), or because it was just downloaded now.
Prints nothing to stdout, writes an error to stderr, exits 1: on any
    failure (no internet, version not available, etc).
"""
import os
import sys

# Reuse runtime_fetch.py's actual fetch/cache logic wholesale rather
# than re-implementing it - this script lives in the same core/
# directory in the source tree, but once embedded into a native
# launcher's C source (see native_launcher.py), it's shipped as a
# standalone string blob, not imported as a package - so it can't
# `from . import runtime_fetch`. native_launcher.py's build step
# concatenates runtime_fetch.py's source ahead of this file's own
# logic before embedding, making runtime_fetch's functions available
# in the same script's global namespace by the time main() runs.


def _find_libpython_dir(python_root: str, major: int, minor: int) -> str:
    """
    python-build-standalone's install_only layout puts the shared
    library under python/lib/libpythonX.Y.so(.1.0) on Linux. Returns
    the directory containing it, or None if not found at that path
    (e.g. a build that turned out to be static-only for some target).
    """
    lib_dir = os.path.join(python_root, "lib")
    if not os.path.isdir(lib_dir):
        return None
    wanted_prefix = f"libpython{major}.{minor}.so"
    for fname in os.listdir(lib_dir):
        if fname.startswith(wanted_prefix):
            return lib_dir
    return None


def main():
    if len(sys.argv) != 3:
        sys.stderr.write("usage: libpython_fetch.py <major> <minor>\n")
        sys.exit(1)
    try:
        major, minor = int(sys.argv[1]), int(sys.argv[2])
    except ValueError:
        sys.stderr.write("major/minor must be integers\n")
        sys.exit(1)

    try:
        # ensure_python() is defined by runtime_fetch.py, concatenated
        # ahead of this script's own source by native_launcher.py's
        # embedding step - see module docstring.
        python_bin = ensure_python(major, minor)  # noqa: F821
    except Exception as e:
        sys.stderr.write(f"gagal siapin runtime Python {major}.{minor}: {e}\n")
        sys.exit(1)

    # python_bin is .../python/bin/python3 - the libpython .so lives
    # two directories up, under .../python/lib/.
    python_root = os.path.dirname(os.path.dirname(python_bin))
    lib_dir = _find_libpython_dir(python_root, major, minor)
    if lib_dir is None:
        sys.stderr.write(
            f"runtime ke-download tapi libpython{major}.{minor}.so gak ketemu "
            f"di {python_root}/lib - struktur archive mungkin beda dari yang diduga.\n"
        )
        sys.exit(1)

    sys.stdout.write(lib_dir + "\n")
    sys.stdout.flush()


if __name__ == "__main__":
    main()
