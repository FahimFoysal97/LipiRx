import argparse
import json
import os
import re
import subprocess
import sys
import tempfile
from pathlib import Path

ROOT_DIR = Path(__file__).resolve().parent

# ==============================================================================
# CONFIGURE PER REPO (or override via release.config.json in repo root)
# ==============================================================================
DEFAULT_CONFIG = {
    # "android" | "kmp" | "flutter"
    "project_type": "kmp",
    # File(s) holding version / versionCode / build number / iOS marketing & build version
    "version_files": [
        "composeApp/build.gradle.kts",
        "iosApp/Configuration/Config.xcconfig",
    ],
    # Release note markdown file (supports {version} and {tag} placeholders)
    "release_notes_file": "RELEASE_NOTES.md",
    # Optional keystore file in repo root; if empty, auto-detects *.jks in root
    "keystore_file": "",
    # Android / KMP APK build command (can be your .bat or gradlew.bat / ./gradlew)
    "build_cmd": "",
    # Expected APK path(s) relative to repo root (first existing match is used)
    "apk_candidates": [
        "composeApp/build/outputs/apk/release/composeApp-release.apk",
        "app/build/outputs/apk/release/app-release.apk",
        "build/app/outputs/flutter-apk/app-release.apk",
    ],
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
    # Git remote name
    "remote": "origin",
}
# ==============================================================================


def normalize_version(raw: str) -> str:
    return raw.strip().lstrip("vV").strip()


def load_config() -> dict:
    cfg = dict(DEFAULT_CONFIG)
    cfg_path = ROOT_DIR / "release.config.json"
    if cfg_path.exists():
        loaded = json.loads(cfg_path.read_text(encoding="utf-8"))
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
        print(f"[ERROR] Command failed in {ROOT_DIR}: {' '.join(cmd)}", file=sys.stderr)
        if result.stderr.strip():
            print(result.stderr.strip(), file=sys.stderr)
        sys.exit(result.returncode)
    return result.stdout.strip()


def run_interactive_shell(command: str, extra_env: dict[str, str] | None = None) -> None:
    """
    Runs build commands attached to the real terminal so password prompts
    (such as keystore passwords in .bat or Gradle) work interactively.
    """
    env = os.environ.copy()
    if extra_env:
        env.update(extra_env)

    if os.name == "nt":
        cmd = ["cmd", "/c", command]
    else:
        cmd = ["sh", "-c", command]

    result = subprocess.run(cmd, cwd=ROOT_DIR, env=env)
    if result.returncode != 0:
        print(
            f"[ERROR] Build command failed with exit code {result.returncode}: {command}",
            file=sys.stderr,
        )
        sys.exit(result.returncode)


def resolve_android_build_cmd(cfg: dict) -> str:
    if cfg.get("build_cmd"):
        return cfg["build_cmd"]
    ptype = cfg.get("project_type", "kmp").lower()
    if ptype == "flutter":
        return "flutter build apk --release"
    gradlew = "gradlew.bat" if os.name == "nt" else "./gradlew"
    return f"{gradlew} assembleRelease"


def find_jks_keystore(cfg: dict) -> Path | None:
    explicit = cfg.get("keystore_file", "").strip()
    if explicit:
        candidate = ROOT_DIR / explicit
        if not candidate.exists():
            print(f"[ERROR] Configured keystore not found: {candidate}", file=sys.stderr)
            sys.exit(1)
        return candidate

    jks_files = sorted(ROOT_DIR.glob("*.jks"))
    if not jks_files:
        return None
    if len(jks_files) > 1:
        print(
            f"[WARN] Multiple .jks files found in root: {[f.name for f in jks_files]}. "
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
        print(
            f"[ERROR] Security check failed: '{rel}' is tracked by Git! "
            "Untrack it and add it to .gitignore before releasing.",
            file=sys.stderr,
        )
        sys.exit(1)


def resolve_notes_path(cfg: dict, version: str, tag: str) -> Path:
    configured = cfg.get("release_notes_file", "RELEASE_NOTES.md")
    return ROOT_DIR / configured.format(version=version, tag=tag)


def extract_version_and_title_from_notes(notes_text: str) -> tuple[str | None, str | None]:
    """
    Extracts version and release title from the top markdown heading, e.g.:
      # v0.4.4 - Persistent Updates & In-App Update Engine Refinements
    """
    for line in notes_text.splitlines():
        stripped = line.strip()
        if not stripped:
            continue
        if not stripped.startswith("#"):
            break
        match = re.match(
            r"^#{1,3}\s+v?([0-9]+(?:\.[0-9]+){1,3}(?:[-+][0-9A-Za-z.-]+)?)\s*(?:[-–—:]\s*(.+))?$",
            stripped,
        )
        if match:
            ver = normalize_version(match.group(1))
            subtitle = (match.group(2) or "").strip()
            title = f"v{ver} - {subtitle}" if subtitle else f"v{ver}"
            return ver, title
        break
    return None, None


def load_and_validate_release_notes(
        cfg: dict, target_version: str, tag: str
) -> tuple[Path, str, str]:
    notes_path = resolve_notes_path(cfg, target_version, tag)
    if not notes_path.exists() or not notes_path.is_file():
        print(f"[ERROR] Release note file not found: {notes_path}", file=sys.stderr)
        sys.exit(1)

    raw_text = notes_path.read_text(encoding="utf-8").strip()
    if not raw_text:
        print(
            f"[ERROR] Release note file '{notes_path.relative_to(ROOT_DIR)}' is empty.",
            file=sys.stderr,
        )
        sys.exit(1)

    file_version, derived_title = extract_version_and_title_from_notes(raw_text)
    if not file_version:
        print(
            f"[ERROR] Could not find a version heading in '{notes_path.relative_to(ROOT_DIR)}'. "
            "Expected top line like '# v0.4.4 - Release Title'.",
            file=sys.stderr,
        )
        sys.exit(1)

    expected_version = normalize_version(target_version)
    if file_version != expected_version:
        print(
            f"[ERROR] Version mismatch: release note file has version '{file_version}', "
            f"but target release version is '{expected_version}'.",
            file=sys.stderr,
        )
        sys.exit(1)

    print(
        f"[OK] Verified {notes_path.relative_to(ROOT_DIR)} matches version {file_version}"
    )
    return notes_path, derived_title or f"v{expected_version}", raw_text


def is_release_already_published(tag: str) -> bool:
    res = subprocess.run(
        ["gh", "release", "view", tag],
        cwd=ROOT_DIR,
        text=True,
        capture_output=True,
    )
    return res.returncode == 0


def prompt_version_if_missing(cli_version: str | None) -> str:
    if cli_version and cli_version.strip():
        ver = normalize_version(cli_version)
        if ver:
            return ver

    while True:
        raw = input("Release version (e.g. 0.4.4 or v0.4.4): ").strip()
        ver = normalize_version(raw)
        if ver:
            return ver
        print("[WARN] Version is required. Please enter a version like 0.4.4.")


def prompt_optional_code(cli_code: int | None) -> int | None:
    if cli_code is not None:
        return cli_code
    while True:
        raw = input("Version code / build number (optional, Enter to skip): ").strip()
        if not raw:
            return None
        if raw.isdigit():
            return int(raw)
        print("[WARN] Please enter a valid integer or press Enter to skip.")


def prompt_optional_title(cli_title: str, derived_title: str) -> str:
    if cli_title and cli_title.strip():
        return cli_title.strip()
    raw = input(
        f"Custom release title (optional, Enter to use '{derived_title}'): "
    ).strip()
    return raw if raw else derived_title


def prompt_optional_ios_toggle(
        cfg: dict, with_ios_flag: bool, skip_ios_flag: bool
) -> bool:
    if with_ios_flag:
        return True
    if skip_ios_flag:
        return False

    current = bool(cfg.get("build_ios", False))
    default_hint = "Y/n" if current else "y/N"
    raw = input(
        f"Build & attach iOS artifact too? (optional, [{default_hint}], Enter for default): "
    ).strip().lower()
    if not raw:
        return current
    if raw in ("y", "yes"):
        return True
    if raw in ("n", "no"):
        return False
    return current


def prompt_optional_path(cli_path: str, label: str) -> str | None:
    if cli_path and cli_path.strip():
        return cli_path.strip()
    raw = input(f"Custom {label} path (optional, Enter to auto-detect): ").strip()
    return raw if raw else None


def update_version_files(cfg: dict, version: str, version_code: int | None) -> None:
    for rel_path in cfg.get("version_files", []):
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
            print(
                f"[OK] Updated {rel_path} -> {version}"
                + (f" (code {version_code})" if version_code is not None else "")
            )


def update_readme(cfg: dict, version: str) -> None:
    readme_rel = cfg.get("readme_file", "README.md")
    readme_path = ROOT_DIR / readme_rel
    if not readme_path.exists():
        return

    content = readme_path.read_text(encoding="utf-8")
    updated = re.sub(
        r"(?mi)^(\*\*(?:Latest Release|Current Version):\*\*\s*)v?[0-9]+\.[0-9]+\.[0-9]+.*$",
        rf"\g<1>v{version}",
        content,
    )
    if updated != content:
        readme_path.write_text(updated, encoding="utf-8")
        print(f"[OK] Updated version line in {readme_rel}")


def find_existing_artifact(
        candidates: list[str],
        explicit_path: str | None,
        label: str,
        required: bool,
) -> Path | None:
    paths = []
    if explicit_path:
        paths.append(ROOT_DIR / explicit_path)
    for rel in candidates:
        paths.append(ROOT_DIR / rel)

    for path in paths:
        if path.exists() and path.is_file():
            return path

    searched = "\n  - ".join(str(p) for p in paths)
    msg = f"{label} artifact not found. Searched:\n  - {searched}"
    if required:
        print(f"[ERROR] {msg}", file=sys.stderr)
        sys.exit(1)
    print(f"[WARN] {msg}")
    return None


def confirm_before_github_release(
        tag: str,
        release_title: str,
        notes_text: str,
        assets: list[Path],
) -> tuple[str, str]:
    """
    Confirmation gate right before creating & publishing the GitHub Release.
    """
    current_title = release_title
    current_notes = notes_text

    while True:
        asset_summary = ", ".join(
            f"{a.name} ({a.stat().st_size / (1024 * 1024):.2f} MB)" for a in assets
        )

        print("\n================ CONFIRM GITHUB RELEASE ================")
        print(f"Tag           : {tag}")
        print(f"Release Title : {current_title}")
        print(f"Assets        : {asset_summary}")
        print("--------------------------------------------------------")
        print(current_notes)
        print("========================================================\n")

        choice = (
            input(
                "Publish this release on GitHub now? "
                "[y = publish / e = edit title & notes / n = cancel]: "
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
                input("Open release notes in editor for this release? (optional, [y/N]): ")
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
            print("[INFO] Cancelled before creating GitHub release.")
            sys.exit(0)


def main() -> None:
    parser = argparse.ArgumentParser(
        description="Root-directory release script for Android, KMP, and Flutter."
    )
    parser.add_argument(
        "version",
        nargs="?",
        default=None,
        help="Release version, e.g. 0.4.4 or v0.4.4 (optional if entered at runtime)",
    )
    parser.add_argument(
        "--code",
        type=int,
        default=None,
        help="Optional versionCode / build number (prompted at runtime if omitted)",
    )
    parser.add_argument(
        "--title",
        default="",
        help="Optional explicit GitHub release title override (prompted at runtime if omitted)",
    )
    parser.add_argument(
        "--apk",
        default="",
        help="Optional explicit APK path (prompted at runtime if omitted)",
    )
    parser.add_argument(
        "--ipa",
        default="",
        help="Optional explicit iOS artifact (.ipa / .zip) path (prompted at runtime if omitted)",
    )
    parser.add_argument(
        "--with-ios",
        action="store_true",
        help="Optional flag to enable iOS build & upload for this run",
    )
    parser.add_argument(
        "--skip-ios",
        action="store_true",
        help="Optional flag to disable iOS build & upload for this run",
    )
    parser.add_argument(
        "--auto-publish",
        action="store_true",
        help="Optional flag to skip pre-release confirmation prompt",
    )
    args = parser.parse_args()

    cfg = load_config()
    version = prompt_version_if_missing(args.version)
    version_code = prompt_optional_code(args.code)
    build_ios = prompt_optional_ios_toggle(cfg, args.with_ios, args.skip_ios)
    explicit_apk = prompt_optional_path(args.apk, "APK")
    explicit_ipa = prompt_optional_path(args.ipa, "iOS artifact") if build_ios else None

    tag = f"v{version}"
    remote = cfg.get("remote", "origin")

    # 1. Validate keystore safety, release note file version, and GitHub release status
    jks_path = find_jks_keystore(cfg)
    guard_keystore_not_tracked(jks_path)
    if jks_path:
        print(f"[OK] Found keystore in root: {jks_path.name}")

    _, derived_title, release_notes = load_and_validate_release_notes(cfg, version, tag)
    release_title = prompt_optional_title(args.title, derived_title)

    if is_release_already_published(tag):
        print(
            f"[ERROR] Release '{tag}' is already published on GitHub. "
            "Use a new version or remove/update the existing release first.",
            file=sys.stderr,
        )
        sys.exit(1)

    # 2. Bump versions & update README, then commit and push
    update_version_files(cfg, version, version_code)
    update_readme(cfg, version)

    run(["git", "add", "-A"])
    has_changes = (
            subprocess.run(["git", "diff", "--cached", "--quiet"], cwd=ROOT_DIR).returncode
            != 0
    )
    if has_changes:
        run(["git", "commit", "-m", f"chore(release): {tag}"])
        run(["git", "push", remote, "HEAD"])

    # 3. Build Android / KMP APK (interactive console so password prompts work)
    extra_build_env = {}
    if jks_path:
        extra_build_env["KEYSTORE_PATH"] = str(jks_path)
        extra_build_env["ANDROID_KEYSTORE_PATH"] = str(jks_path)

    android_cmd = resolve_android_build_cmd(cfg)
    print(f"[INFO] Building APK: {android_cmd}")
    run_interactive_shell(android_cmd, extra_env=extra_build_env)
    apk_path = find_existing_artifact(
        cfg.get("apk_candidates", []), explicit_apk, "APK", required=True
    )
    assert apk_path is not None
    print(f"[OK] APK ready: {apk_path}")

    assets: list[Path] = [apk_path]

    # 4. Optionally build iOS artifact
    if build_ios:
        ios_cmd = cfg.get("ios_build_cmd", "").strip()
        if ios_cmd:
            print(f"[INFO] Building iOS artifact: {ios_cmd}")
            run_interactive_shell(ios_cmd, extra_env=extra_build_env)
        ipa_path = find_existing_artifact(
            cfg.get("ios_artifact_candidates", []),
            explicit_ipa,
            "iOS",
            required=bool(cfg.get("require_ios_artifact", True)),
        )
        if ipa_path:
            print(f"[OK] iOS artifact ready: {ipa_path}")
            assets.append(ipa_path)

    # 5. Explicit confirmation before tagging and publishing on GitHub
    if not args.auto_publish:
        release_title, release_notes = confirm_before_github_release(
            tag, release_title, release_notes, assets
        )

    # 6. Create & push matching Git tag, then publish GitHub Release with notes & assets
    with tempfile.NamedTemporaryFile(
            mode="w", suffix=".md", delete=False, encoding="utf-8"
    ) as notes_tmp:
        notes_tmp.write(release_notes + "\n")
        notes_tmp_path = Path(notes_tmp.name)

    try:
        run(["git", "tag", "-a", tag, "-m", f"Release {tag}"])
        run(["git", "push", remote, tag])

        gh_cmd = [
            "gh",
            "release",
            "create",
            tag,
            *[str(a) for a in assets],
            "--title",
            release_title,
            "--notes-file",
            str(notes_tmp_path),
        ]
        run(gh_cmd)
    finally:
        if notes_tmp_path.exists():
            notes_tmp_path.unlink(missing_ok=True)

    uploaded_names = ", ".join(a.name for a in assets)
    print(
        f"\n[DONE] Published GitHub release {tag} ('{release_title}') with: {uploaded_names}"
    )


if __name__ == "__main__":
    main()