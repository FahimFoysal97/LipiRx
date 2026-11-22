import argparse
import json
import os
import re
import shutil
import subprocess
import sys
import tempfile
import time
import urllib.request
import urllib.error
from pathlib import Path
from typing import Optional, Tuple

# Enable VT100 / ANSI escape sequences in Windows console
if os.name == "nt":
    os.system("")
    if hasattr(sys.stdout, "reconfigure"):
        sys.stdout.reconfigure(encoding="utf-8")

ROOT_DIR = Path(__file__).resolve().parent

# ==============================================================================
# CONFIGURE PER REPO (or override via release.config.json in repo root)
# ==============================================================================
DEFAULT_CONFIG = {
    # "android" | "kmp" | "flutter"
    "project_type": "kmp",
    # File(s) holding version / versionCode / build number / iOS marketing & build version
    "version_files": [
        "androidApp/module.yaml",
        "composeApp/build.gradle.kts",
        "iosApp/Configuration/Config.xcconfig",
    ],
    # Whether release.py updates version_files locally before building (never commits/pushes)
    "sync_version_files": False,
    # Release note markdown file (supports {version} and {tag} placeholders)
    "release_notes_file": "RELEASE_NOTES.md",
    # Optional keystore file in repo root; if empty, auto-detects *.jks in root
    "keystore_file": "",
    # Build command (supports {keystore}, {version}, {tag})
    "build_cmd": "sign-release.bat release",
    # Expected APK path(s) or glob pattern(s) relative to repo root
    "apk_candidates": [
        "apks/LipiRx-v*-release-signed.apk",
        "apks/LipiRx-v*-release.apk",
        "apks/*.apk",
    ],
    # Publish as a GitHub pre-release by default so you can finalize on the web UI
    "prerelease": True,
    # Skip GitHub release confirmation prompt if True
    "auto_publish": False,
    # Optional iOS build support (typically true on macOS with Xcode)
    "build_ios": False,
    "ios_build_cmd": "",
    "ios_artifact_candidates": [
        "build/ios/ipa/App.ipa",
        "iosApp/build/App.ipa",
    ],
    "require_ios_artifact": True,
    # README path relative to repo root
    "readme_file": "README.md",
    # Git remote name (used for checking existing tags and pushing the release tag)
    "remote": "origin",
}
# ==============================================================================


class Color:
    RESET = "\033[0m"
    BOLD = "\033[1m"
    DIM = "\033[2m"
    RED = "\033[91m"
    GREEN = "\033[92m"
    YELLOW = "\033[93m"
    BLUE = "\033[94m"
    MAGENTA = "\033[95m"
    CYAN = "\033[96m"
    WHITE = "\033[97m"


def log_ok(msg: str) -> None:
    print(f"{Color.GREEN}{Color.BOLD}✔ [OK]{Color.RESET} {msg}")


def log_info(msg: str) -> None:
    print(f"{Color.CYAN}{Color.BOLD}ℹ [INFO]{Color.RESET} {msg}")


def log_warn(msg: str) -> None:
    print(f"{Color.YELLOW}{Color.BOLD}⚠ [WARN]{Color.RESET} {msg}", file=sys.stderr)


def log_err(msg: str) -> None:
    print(f"{Color.RED}{Color.BOLD}✖ [ERROR]{Color.RESET} {msg}", file=sys.stderr)


def print_banner(title: str, subtitle: str = "") -> None:
    width = 64
    print(f"\n{Color.CYAN}{Color.BOLD}┌{'─' * (width - 2)}┐{Color.RESET}")
    print(f"{Color.CYAN}{Color.BOLD}│  {title.center(width - 6)}  │{Color.RESET}")
    if subtitle:
        print(f"{Color.CYAN}│  {subtitle.center(width - 6)}  │{Color.RESET}")
    print(f"{Color.CYAN}{Color.BOLD}└{'─' * (width - 2)}┘{Color.RESET}\n")


def normalize_version(raw: str) -> str:
    return raw.strip().lstrip("vV").strip()


def load_config() -> dict:
    cfg = dict(DEFAULT_CONFIG)
    cfg_path = ROOT_DIR / "release.config.json"
    if cfg_path.exists():
        loaded = json.loads(cfg_path.read_text(encoding="utf-8"))
        if not isinstance(loaded, dict):
            log_err("release.config.json must contain a JSON object.")
            sys.exit(1)
        cfg.update(loaded)
    return cfg


def run(cmd: list[str], check: bool = True) -> str:
    result = subprocess.run(
        cmd,
        cwd=ROOT_DIR,
        text=True,
        capture_output=True,
    )
    if check and result.returncode != 0:
        log_err(f"Command failed in {ROOT_DIR}: {' '.join(cmd)}")
        if result.stderr.strip():
            print(f"{Color.RED}{result.stderr.strip()}{Color.RESET}", file=sys.stderr)
        sys.exit(result.returncode)
    return result.stdout.strip()


def run_interactive_shell(command: str, extra_env: Optional[dict] = None) -> None:
    """
    Runs build commands attached to the real terminal so interactive scripts
    can prompt or read environment variables directly when needed.
    """
    env = os.environ.copy()
    env["FROM_RELEASE_PY"] = "1"
    if extra_env:
        env.update(extra_env)

    if os.name == "nt":
        cmd = ["cmd", "/d", "/s", "/c", command]
    else:
        cmd = ["sh", "-c", command]

    result = subprocess.run(cmd, cwd=ROOT_DIR, env=env)
    if result.returncode != 0:
        log_err(f"Build command failed with exit code {result.returncode}: {command}")
        sys.exit(result.returncode)


def resolve_android_build_cmd(
    cfg: dict,
    version: str = "",
    tag: str = "",
    keystore_path: Optional[Path] = None,
) -> str:
    raw_cmd = str(cfg.get("build_cmd", "")).strip()
    if raw_cmd:
        return raw_cmd.format(
            version=version,
            tag=tag,
            keystore=str(keystore_path) if keystore_path else "",
        )

    ptype = str(cfg.get("project_type", "kmp")).lower()
    if ptype == "flutter":
        return "flutter build apk --release"

    if os.name == "nt":
        for candidate in [
            ROOT_DIR / "gradlew.bat",
            ROOT_DIR / "android" / "gradlew.bat",
            ROOT_DIR.parent / "gradlew.bat",
        ]:
            if candidate.exists():
                return f'"{candidate}" assembleRelease'
        return "gradlew.bat assembleRelease"

    return "./gradlew assembleRelease"


def resolve_gh_executable() -> str | None:
    found = shutil.which("gh")
    if found:
        return found
    common_win = Path(r"C:\Program Files\GitHub CLI\gh.exe")
    if common_win.exists():
        return str(common_win)
    return None


def find_jks_keystore(cfg: dict) -> Path | None:
    explicit = str(cfg.get("keystore_file", "")).strip()
    if explicit:
        candidate = ROOT_DIR / explicit
        if not candidate.exists():
            log_err(f"Configured keystore not found: {candidate}")
            sys.exit(1)
        return candidate

    jks_files = sorted(ROOT_DIR.glob("*.jks"))
    if not jks_files:
        return None
    if len(jks_files) > 1:
        log_warn(
            f"Multiple .jks files found in root: {[f.name for f in jks_files]}. "
            f"Using {jks_files[0].name}"
        )
    return jks_files[0]


def guard_keystore_not_tracked(jks_path: Path | None) -> None:
    if not jks_path:
        return
    rel = jks_path.relative_to(ROOT_DIR).as_posix()
    tracked = (
        subprocess.run(
            ["git", "ls-files", "--error-unmatch", rel],
            cwd=ROOT_DIR,
            capture_output=True,
            text=True,
        ).returncode
        == 0
    )
    if tracked:
        log_err(
            f"Security check failed: '{rel}' is tracked by Git! "
            "Untrack it and add it to .gitignore before releasing."
        )
        sys.exit(1)


def resolve_notes_path(cfg: dict) -> Path:
    configured = cfg.get("release_notes_file", "RELEASE_NOTES.md")
    return ROOT_DIR / configured


def parse_release_note_metadata(notes_text: str) -> tuple[str, str, str, int | None]:
    """
    Parses RELEASE_NOTES.md where:
    - Line 1 is the Title (e.g. '# v0.5.1 - Navigation 3 Architecture...')
    - Line 2 onwards is the Description/Body section for GitHub Release
    Returns: (version, release_title, description_body, version_code)
    """
    lines = notes_text.strip().splitlines()
    if not lines:
        return "", "", "", None

    first_line = lines[0].strip()
    clean_title = re.sub(r"^#+\s*", "", first_line).strip()

    version_match = re.search(r"\bv?([0-9]+(?:\.[0-9]+){1,3}(?:[-+][0-9A-Za-z.-]+)?)\b", clean_title)
    version = normalize_version(version_match.group(1)) if version_match else ""

    body_lines = lines[1:]
    while body_lines and not body_lines[0].strip():
        body_lines.pop(0)
    description_body = "\n".join(body_lines).strip()

    code = None
    code_match = re.search(
        r"(?mi)^\s*(?:version[_\s-]*code|build[_\s-]*number)\s*[:=]\s*(\d+)\s*$",
        notes_text,
    )
    if not code_match:
        code_match = re.search(
            r"(?i)\(\s*(?:version[_\s-]*code|build)\s*[:#-]?\s*(\d+)\s*\)",
            notes_text,
        )
    if code_match:
        code = int(code_match.group(1))

    return version, clean_title, description_body, code


def load_release_notes(cfg: dict) -> tuple[Path, str, str, str, int | None]:
    notes_path = resolve_notes_path(cfg)
    if not notes_path.exists() or not notes_path.is_file():
        log_err(f"Release note file not found: {notes_path}")
        sys.exit(1)

    raw_text = notes_path.read_text(encoding="utf-8").strip()
    if not raw_text:
        log_err(f"Release note file '{notes_path.relative_to(ROOT_DIR)}' is empty.")
        sys.exit(1)

    version, derived_title, description_body, code = parse_release_note_metadata(raw_text)
    if not version:
        log_err(
            f"Could not find a version heading in '{notes_path.relative_to(ROOT_DIR)}'. "
            "Expected a heading like '# v0.5.2 - Release Title'."
        )
        sys.exit(1)

    log_ok(
        f"Read version {version} and title '{derived_title}' from {notes_path.relative_to(ROOT_DIR)}"
        + (f" (code {code})" if code is not None else "")
    )
    return notes_path, version, derived_title, description_body, code


def extract_readme_versions(readme_text: str) -> list[str]:
    explicit_matches = re.findall(
        r"(?mi)^.*?\b(?:version|release|latest release|current version)\b[^\n]*?\bv?([0-9]+(?:\.[0-9]+){1,3})\b",
        readme_text,
    )
    if explicit_matches:
        return [normalize_version(v) for v in explicit_matches]

    all_semver = re.findall(r"\bv?([0-9]+(?:\.[0-9]+){1,3})\b", readme_text)
    return [normalize_version(v) for v in all_semver]


def verify_readme_matches_version(cfg: dict, release_version: str) -> None:
    readme_rel = cfg.get("readme_file", "README.md")
    readme_path = ROOT_DIR / readme_rel
    if not readme_path.exists():
        log_err(f"README file '{readme_rel}' not found; cannot verify version match.")
        sys.exit(1)

    readme_text = readme_path.read_text(encoding="utf-8")
    readme_versions = extract_readme_versions(readme_text)

    if not readme_versions:
        log_err(f"No version found in '{readme_rel}' to match release note version '{release_version}'.")
        sys.exit(1)

    expected = normalize_version(release_version)
    if expected not in readme_versions:
        found_str = ", ".join(dict.fromkeys(readme_versions))
        log_err(
            f"Version mismatch between release note and {readme_rel}: "
            f"release note has '{expected}', but {readme_rel} has '{found_str}'."
        )
        sys.exit(1)

    log_ok(f"Verified {readme_rel} matches release note version {expected}")


def is_local_tag_existing(tag: str) -> bool:
    res = subprocess.run(
        ["git", "rev-parse", "--verify", f"refs/tags/{tag}"],
        cwd=ROOT_DIR,
        text=True,
        capture_output=True,
    )
    return res.returncode == 0


def is_tag_on_remote(remote: str, tag: str) -> bool:
    res = subprocess.run(
        ["git", "ls-remote", "--tags", remote, f"refs/tags/{tag}"],
        cwd=ROOT_DIR,
        text=True,
        capture_output=True,
    )
    return res.returncode == 0 and bool(res.stdout.strip())


def is_release_already_published(tag: str, remote: str) -> bool:
    if is_tag_on_remote(remote, tag):
        return True

    gh_exe = resolve_gh_executable()
    if not gh_exe:
        log_warn(
            "GitHub CLI ('gh') was not found on PATH. "
            "Install it ('winget install --id GitHub.cli') and run 'gh auth login' "
            "so release.py can publish directly to GitHub Releases."
        )
        return is_local_tag_existing(tag)

    res = subprocess.run(
        [gh_exe, "release", "view", tag],
        cwd=ROOT_DIR,
        text=True,
        capture_output=True,
    )
    return res.returncode == 0


def update_version_files(cfg: dict, version: str, version_code: int | None) -> None:
    version_files = cfg.get("version_files", [])
    if not isinstance(version_files, list):
        log_err("'version_files' in config must be a list.")
        sys.exit(1)

    for rel_path in version_files:
        path = ROOT_DIR / rel_path
        if not path.exists():
            continue

        original = path.read_text(encoding="utf-8")
        updated = original

        if path.name == "pubspec.yaml":
            if version_code is not None:
                updated = re.sub(
                    r"(?m)^version:\s*[^\s#]+",
                    f"version: {version}+{version_code}",
                    updated,
                )
            else:
                updated = re.sub(
                    r"(?m)^version:\s*([0-9]+\.[0-9]+\.[0-9]+)(\+[0-9]+)?",
                    lambda m: f"version: {version}{m.group(2) or ''}",
                    updated,
                )
        elif path.name == "module.yaml":
            updated = re.sub(
                r'(?m)^(\s*versionName\s*:\s*["\']?)([^"\'\s#]+)(["\']?)',
                rf"\g<1>{version}\g<3>",
                updated,
            )
            if version_code is not None:
                updated = re.sub(
                    r"(?m)^(\s*versionCode\s*:\s*)(\d+)",
                    rf"\g<1>{version_code}",
                    updated,
                )
        elif path.suffix == ".xcconfig":
            updated = re.sub(
                r"(?m)^(\s*MARKETING_VERSION\s*=\s*)[^\s#]+",
                rf"\g<1>{version}",
                updated,
            )
            if version_code is not None:
                updated = re.sub(
                    r"(?m)^(\s*CURRENT_PROJECT_VERSION\s*=\s*)\d+",
                    rf"\g<1>{version_code}",
                    updated,
                )
        else:
            updated = re.sub(
                r'(?m)^(\s*(?:versionName|version)\s*[:=]\s*["\'])([^"\']+)(["\'])',
                rf"\g<1>{version}\g<3>",
                updated,
            )
            if version_code is not None:
                updated = re.sub(
                    r"(?m)^(\s*versionCode\s*[:=]\s*)(\d+)",
                    rf"\g<1>{version_code}",
                    updated,
                )

        if updated != original:
            path.write_text(updated, encoding="utf-8")
            log_ok(
                f"Updated {rel_path} -> {version}"
                + (f" (code {version_code})" if version_code is not None else "")
            )


def find_existing_artifact(
    candidates: list[str],
    explicit_path: str | None,
    label: str,
    required: bool,
) -> Path | None:
    patterns: list[str] = []
    if explicit_path:
        patterns.append(explicit_path)
    patterns.extend(candidates)

    searched_paths: list[str] = []
    for rel in patterns:
        target = ROOT_DIR / rel
        searched_paths.append(str(target))
        if any(ch in rel for ch in ("*", "?", "[")):
            matches = [p for p in ROOT_DIR.glob(rel) if p.is_file()]
            if matches:
                matches.sort(key=lambda p: p.stat().st_mtime, reverse=True)
                return matches[0]
        elif target.exists() and target.is_file():
            return target

    searched = "\n  - ".join(searched_paths)
    msg = f"{label} artifact not found. Searched:\n  - {searched}"
    if required:
        log_err(msg)
        sys.exit(1)
    log_warn(msg)
    return None


def confirm_before_github_release(
    tag: str,
    release_title: str,
    notes_text: str,
    assets: list[Path],
    is_prerelease: bool,
) -> tuple[str, str]:
    current_title = release_title
    current_notes = notes_text

    while True:
        asset_summary = ", ".join(
            f"{Color.GREEN}{a.name}{Color.RESET} ({a.stat().st_size / (1024 * 1024):.2f} MB)" for a in assets
        )
        release_kind = f"{Color.YELLOW}Pre-release{Color.RESET}" if is_prerelease else f"{Color.GREEN}Full Release{Color.RESET}"

        print(f"\n{Color.CYAN}{Color.BOLD}╔════════════════════════════════════════════════════════════════╗{Color.RESET}")
        print(f"{Color.CYAN}{Color.BOLD}║                  CONFIRM GITHUB RELEASE                        ║{Color.RESET}")
        print(f"{Color.CYAN}{Color.BOLD}╠════════════════════════════════════════════════════════════════╣{Color.RESET}")
        print(f"{Color.CYAN}║{Color.RESET} {Color.BOLD}Tag          {Color.RESET} : {Color.CYAN}{tag}{Color.RESET}")
        print(f"{Color.CYAN}║{Color.RESET} {Color.BOLD}Release Type {Color.RESET} : {release_kind}")
        print(f"{Color.CYAN}║{Color.RESET} {Color.BOLD}Release Title{Color.RESET} : {Color.BOLD}{current_title}{Color.RESET}")
        print(f"{Color.CYAN}║{Color.RESET} {Color.BOLD}Assets       {Color.RESET} : {asset_summary}")
        print(f"{Color.CYAN}{Color.BOLD}╟────────────────────────────────────────────────────────────────╢{Color.RESET}")
        for line in current_notes.splitlines():
            print(f"{Color.CYAN}║{Color.RESET} {line}")
        print(f"{Color.CYAN}{Color.BOLD}╚════════════════════════════════════════════════════════════════╝{Color.RESET}\n")

        choice = (
            input(
                f"{Color.BOLD}Publish this release on GitHub now? "
                f"[{Color.GREEN}y{Color.RESET} = publish / {Color.YELLOW}e{Color.RESET} = edit / {Color.RED}n{Color.RESET} = cancel]: {Color.RESET}"
            )
            .strip()
            .lower()
        )
        if choice == "y":
            return current_title, current_notes
        if choice == "e":
            new_title = input(
                f"New release title (optional, Enter to keep '{current_title}'): "
            ).strip()
            if new_title:
                current_title = new_title

            open_editor = (
                input(
                    "Open release notes in editor for this release? (optional, [y/N]): "
                )
                .strip()
                .lower()
            )
            if open_editor == "y":
                with tempfile.NamedTemporaryFile(
                    mode="w", suffix=".md", delete=False, encoding="utf-8"
                ) as tmp:
                    tmp.write(current_notes + "\n")
                    tmp_path = Path(tmp.name)
                try:
                    if os.name == "nt":
                        subprocess.run(["notepad", str(tmp_path)], check=False)
                    else:
                        editor = os.environ.get("EDITOR", "nano")
                        subprocess.run([editor, str(tmp_path)], check=False)
                    edited = tmp_path.read_text(encoding="utf-8").strip()
                    if edited:
                        current_notes = edited
                finally:
                    if tmp_path.exists():
                        tmp_path.unlink(missing_ok=True)
        else:
            log_info("Cancelled before creating GitHub release.")
            sys.exit(0)


class ProgressFileWrapper:
    def __init__(self, file_path: Path, progress_callback):
        self.file_path = file_path
        self.total_size = file_path.stat().st_size
        self.bytes_read = 0
        self.progress_callback = progress_callback
        self.file_obj = open(file_path, "rb")

    def read(self, chunk_size=65536):
        data = self.file_obj.read(chunk_size)
        if data:
            self.bytes_read += len(data)
            self.progress_callback(self.bytes_read, self.total_size)
        return data

    def close(self):
        self.file_obj.close()


def render_upload_progress(
    filename: str,
    uploaded_bytes: int,
    total_bytes: int,
    start_time: float,
    bar_width: int = 30,
) -> None:
    elapsed = max(time.time() - start_time, 0.001)
    fraction = min(uploaded_bytes / max(total_bytes, 1), 1.0)
    filled_len = int(bar_width * fraction)

    bar = f"{Color.GREEN}{'━' * filled_len}{Color.CYAN}{'•' * (bar_width - filled_len)}{Color.RESET}"
    mb_uploaded = uploaded_bytes / (1024 * 1024)
    mb_total = total_bytes / (1024 * 1024)
    percent = fraction * 100

    speed_mbps = (uploaded_bytes / (1024 * 1024)) / elapsed
    remaining_bytes = max(total_bytes - uploaded_bytes, 0)
    eta_sec = int(remaining_bytes / max(uploaded_bytes / elapsed, 1)) if uploaded_bytes > 0 else 0
    eta_str = f"{eta_sec // 60:02d}:{eta_sec % 60:02d}"

    sys.stdout.write(
        f"\r[ {bar} ] {Color.BOLD}{mb_uploaded:.1f} MB{Color.RESET} / {mb_total:.1f} MB "
        f"({Color.YELLOW}{percent:.1f}%{Color.RESET})  "
        f"{Color.GREEN}{speed_mbps:.1f} MB/s{Color.RESET}  ETA {eta_str}  \033[K"
    )
    sys.stdout.flush()


def upload_asset_with_progress(
    gh_exe: str,
    tag: str,
    asset_path: Path,
) -> bool:
    filename = asset_path.name
    total_bytes = asset_path.stat().st_size
    mb_size = total_bytes / (1024 * 1024)

    token = run([gh_exe, "auth", "token"], check=False).strip()
    repo_info = run(["git", "config", "--get", "remote.origin.url"], check=False).strip()
    match = re.search(r"github\.com[:/]([^/]+)/([^/\.]+)(?:\.git)?$", repo_info)

    if token and match:
        owner, repo = match.group(1), match.group(2)
        rel_view = run([gh_exe, "release", "view", tag, "--json", "uploadUrl"], check=False)
        upload_url_template = ""
        if rel_view:
            try:
                upload_url_template = json.loads(rel_view).get("uploadUrl", "")
            except Exception:
                pass

        if upload_url_template:
            clean_upload_url = upload_url_template.split("{")[0] + f"?name={filename}"
            start_time = time.time()
            log_info(f"Uploading {filename} ({mb_size:.2f} MB)...")

            try:
                wrapper = ProgressFileWrapper(
                    asset_path,
                    lambda read_b, total_b: render_upload_progress(filename, read_b, total_b, start_time)
                )

                req = urllib.request.Request(
                    clean_upload_url,
                    data=wrapper,
                    headers={
                        "Authorization": f"Bearer {token}",
                        "Content-Type": "application/octet-stream",
                        "Content-Length": str(total_bytes),
                        "User-Agent": "LipiRx-Release-Uploader/1.0",
                    },
                    method="POST",
                )

                with urllib.request.urlopen(req) as resp:
                    wrapper.close()
                    elapsed = max(time.time() - start_time, 0.1)
                    speed = mb_size / elapsed
                    sys.stdout.write("\n")
                    log_ok(f"Uploaded {filename} ({mb_size:.2f} MB) in {elapsed:.1f}s ({speed:.1f} MB/s)")
                    return True
            except Exception as ex:
                sys.stdout.write("\n")
                log_warn(f"Direct API upload fallback to gh CLI: {ex}")

    log_info(f"Uploading {filename} ({mb_size:.2f} MB) via GitHub CLI...")
    start_time = time.time()
    res = subprocess.run([gh_exe, "release", "upload", tag, str(asset_path), "--clobber"], capture_output=True, text=True)
    elapsed = max(time.time() - start_time, 0.1)
    if res.returncode == 0:
        log_ok(f"Uploaded {filename} ({mb_size:.2f} MB) in {elapsed:.1f}s")
        return True
    else:
        log_err(f"Failed to upload {filename}: {res.stderr.strip()}")
        return False


def main() -> None:
    print_banner("LipiRx Multi-Platform Release Engine", "KMP • Android • iOS • MCP Protocol")

    parser = argparse.ArgumentParser(
        description="Root-directory release script for Android, KMP, and Flutter."
    )
    parser.add_argument(
        "--code",
        type=int,
        default=None,
        help="Optional versionCode / build number override",
    )
    parser.add_argument(
        "--title",
        default="",
        help="Optional explicit GitHub release title override",
    )
    parser.add_argument(
        "--apk",
        default="",
        help="Optional explicit APK path",
    )
    parser.add_argument(
        "--ipa",
        default="",
        help="Optional explicit iOS artifact (.ipa / .zip) path",
    )
    parser.add_argument(
        "--full-release",
        action="store_true",
        help="Publish as a full release instead of a pre-release",
    )
    parser.add_argument(
        "--with-ios",
        action="store_true",
        help="Enable iOS build & upload for this run",
    )
    parser.add_argument(
        "--skip-ios",
        action="store_true",
        help="Disable iOS build & upload for this run",
    )
    parser.add_argument(
        "--auto-publish",
        action="store_true",
        help="Skip GitHub release confirmation prompt",
    )
    args = parser.parse_args()

    cfg = load_config()
    remote = cfg.get("remote", "origin")
    is_prerelease = False if args.full_release else bool(cfg.get("prerelease", True))
    auto_publish = bool(args.auto_publish or cfg.get("auto_publish", False))

    # 1. Keystore guard, release notes version read, README version check, and already-published check
    jks_path = find_jks_keystore(cfg)
    guard_keystore_not_tracked(jks_path)
    if jks_path:
        log_ok(f"Found keystore in root: {jks_path.name}")

    _, version, derived_title, release_notes, parsed_code = load_release_notes(cfg)
    verify_readme_matches_version(cfg, version)

    tag = f"v{version}"
    if is_release_already_published(tag, remote):
        log_err(
            f"Release '{tag}' is already published on GitHub or tag exists on '{remote}'. "
            "Update your release note & README version first."
        )
        sys.exit(1)

    version_code = args.code if args.code is not None else parsed_code
    release_title = (
        args.title.strip() if args.title and args.title.strip() else derived_title
    )
    build_ios = (
        True
        if args.with_ios
        else (False if args.skip_ios else bool(cfg.get("build_ios", False)))
    )
    explicit_apk = args.apk.strip() if args.apk and args.apk.strip() else None
    explicit_ipa = (
        args.ipa.strip() if (build_ios and args.ipa and args.ipa.strip()) else None
    )

    # 2. Optional local version file sync (never commits or pushes branch changes)
    if cfg.get("sync_version_files", False):
        update_version_files(cfg, version, version_code)

    # 3. Build Android / KMP APK
    extra_build_env: dict[str, str] = {}
    if jks_path:
        extra_build_env["KEYSTORE_PATH"] = str(jks_path)
        extra_build_env["ANDROID_KEYSTORE_PATH"] = str(jks_path)

    android_cmd = resolve_android_build_cmd(
        cfg,
        version=version,
        tag=tag,
        keystore_path=jks_path,
    )
    log_info(f"Building APK: {android_cmd}")
    run_interactive_shell(android_cmd, extra_env=extra_build_env)
    apk_path = find_existing_artifact(
        cfg.get("apk_candidates", []), explicit_apk, "APK", required=True
    )
    assert apk_path is not None
    log_ok(f"APK ready: {apk_path}")

    assets: list[Path] = [apk_path]

    # 4. Optionally build iOS artifact
    if build_ios:
        ios_cmd = str(cfg.get("ios_build_cmd", "")).strip()
        if ios_cmd:
            log_info(f"Building iOS artifact: {ios_cmd}")
            run_interactive_shell(ios_cmd, extra_env=extra_build_env)
        ipa_path = find_existing_artifact(
            cfg.get("ios_artifact_candidates", []),
            explicit_ipa,
            "iOS",
            required=bool(cfg.get("require_ios_artifact", True)),
        )
        if ipa_path:
            log_ok(f"iOS artifact ready: {ipa_path}")
            assets.append(ipa_path)

    # 5. Explicit confirmation before tagging and publishing on GitHub
    if not auto_publish:
        release_title, release_notes = confirm_before_github_release(
            tag, release_title, release_notes, assets, is_prerelease
        )

    # 6. Create & push matching Git tag, then publish GitHub release with notes & assets
    gh_exe = resolve_gh_executable()
    if not gh_exe:
        log_err(
            "GitHub CLI ('gh') is required to publish the release. "
            "Install it ('winget install --id GitHub.cli') and run 'gh auth login'."
        )
        sys.exit(1)

    with tempfile.NamedTemporaryFile(
        mode="w", suffix=".md", delete=False, encoding="utf-8"
    ) as notes_tmp:
        notes_tmp.write(release_notes + "\n")
        notes_tmp_path = Path(notes_tmp.name)

    try:
        if not is_local_tag_existing(tag):
            run(["git", "tag", "-a", tag, "-m", f"Release {tag}"])
        run(["git", "push", remote, tag])

        gh_cmd = [
            gh_exe,
            "release",
            "create",
            tag,
            *[str(a) for a in assets],
            "--title",
            release_title,
            "--notes-file",
            str(notes_tmp_path),
        ]
        if is_prerelease:
            gh_cmd.append("--prerelease")
        run(gh_cmd)

        for asset_path in assets:
            upload_asset_with_progress(gh_exe, tag, asset_path)
    finally:
        if notes_tmp_path.exists():
            notes_tmp_path.unlink(missing_ok=True)

    uploaded_names = ", ".join(a.name for a in assets)
    label_kind = "pre-release" if is_prerelease else "release"
    print(
        f"\n{Color.GREEN}{Color.BOLD}✨ [DONE] Published GitHub {label_kind} {tag} ('{release_title}') with: {uploaded_names}{Color.RESET}\n"
    )


if __name__ == "__main__":
    main()
