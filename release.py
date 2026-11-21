import argparse
import json
import os
import re
import shutil
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
    # Android / KMP APK build command (e.g. your .bat file or gradlew.bat)
    "build_cmd": "sign-release.bat",
    # Expected APK path(s) or glob pattern(s) relative to repo root
    "apk_candidates": [
        "apks/*.apk",
        "apks/composeApp-release.apk",
        "apks/app-release.apk",
    ],
    # Publish as a GitHub pre-release by default so you can finalize on the web UI
    "prerelease": True,
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


def resolve_notes_path(cfg: dict) -> Path:
    configured = cfg.get("release_notes_file", "RELEASE_NOTES.md")
    return ROOT_DIR / configured


def parse_release_note_metadata(notes_text: str) -> tuple[str | None, str | None, int | None]:
    """
    Extracts:
      - version (from top markdown heading, e.g. '# v0.4.4 - Title')
      - release_title (e.g. 'v0.4.4 - Title')
      - optional version_code if present in heading or metadata line (e.g. 'code: 44' or '(build 44)')
    """
    version: str | None = None
    title: str | None = None
    code: int | None = None

    for line in notes_text.splitlines():
        stripped = line.strip()
        if not stripped:
            continue
        if stripped.startswith("#"):
            match = re.match(
                r"^#{1,3}\s+v?([0-9]+(?:\.[0-9]+){1,3}(?:[-+][0-9A-Za-z.-]+)?)\s*(?:[-–—:]\s*(.+))?$",
                stripped,
            )
            if match:
                version = normalize_version(match.group(1))
                subtitle = (match.group(2) or "").strip()
                title = f"v{version} - {subtitle}" if subtitle else f"v{version}"
            break
        break

    code_match = re.search(
        r"(?mi)^\s*(?:version[_\s-]*code|build(?:\s*number)?)\s*[:=]\s*(\d+)\s*$",
        notes_text,
    )
    if not code_match:
        code_match = re.search(
            r"(?i)\b(?:code|build)\s*[:#-]?\s*(\d+)\b",
            notes_text.splitlines()[0] if notes_text.splitlines() else "",
        )
    if code_match:
        code = int(code_match.group(1))

    return version, title, code


def load_release_notes(cfg: dict) -> tuple[Path, str, str, str, int | None]:
    notes_path = resolve_notes_path(cfg)
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

    version, derived_title, code = parse_release_note_metadata(raw_text)
    if not version:
        print(
            f"[ERROR] Could not find a version heading in '{notes_path.relative_to(ROOT_DIR)}'. "
            "Expected top line like '# v0.4.4 - Release Title'.",
            file=sys.stderr,
        )
        sys.exit(1)

    print(
        f"[OK] Read version {version} from {notes_path.relative_to(ROOT_DIR)}"
        + (f" (code {code})" if code is not None else "")
    )
    return notes_path, version, derived_title or f"v{version}", raw_text, code


def extract_readme_versions(readme_text: str) -> list[str]:
    """
    Extracts version strings from README.md (prioritizing explicit version/release lines,
    then falling back to semver-like tokens).
    """
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
        print(
            f"[ERROR] README file '{readme_rel}' not found; cannot verify version match with release notes.",
            file=sys.stderr,
        )
        sys.exit(1)

    readme_text = readme_path.read_text(encoding="utf-8")
    readme_versions = extract_readme_versions(readme_text)

    if not readme_versions:
        print(
            f"[ERROR] No version found in '{readme_rel}' to match release note version '{release_version}'.",
            file=sys.stderr,
        )
        sys.exit(1)

    expected = normalize_version(release_version)
    if expected not in readme_versions:
        found_str = ", ".join(dict.fromkeys(readme_versions))
        print(
            f"[ERROR] Version mismatch between release note and {readme_rel}: "
            f"release note has '{expected}', but {readme_rel} has '{found_str}'.",
            file=sys.stderr,
        )
        sys.exit(1)

    print(f"[OK] Verified {readme_rel} matches release note version {expected}")


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
        print(
            "[WARN] GitHub CLI ('gh') was not found on PATH. "
            "Install it ('winget install --id GitHub.cli') and run 'gh auth login' "
            "so release.py can publish directly to GitHub Releases."
        )
        return False

    res = subprocess.run(
        [gh_exe, "release", "view", tag],
        cwd=ROOT_DIR,
        text=True,
        capture_output=True,
    )
    return res.returncode == 0


def prompt_optional_code(cli_code: int | None, parsed_code: int | None) -> int | None:
    if cli_code is not None:
        return cli_code
    default_hint = f"default {parsed_code}, " if parsed_code is not None else ""
    while True:
        raw = input(
            f"Version code / build number (optional, {default_hint}Enter to skip): "
        ).strip()
        if not raw:
            return parsed_code
        if raw.isdigit():
            return int(raw)
        print("[WARN] Please enter a valid integer or press Enter to skip.")


def prompt_optional_title(cli_title: str, derived_title: str) -> str:
    if cli_title and cli_title.strip():
        return cli_title.strip()
    raw = input(
        f"Custom release title (optional, Enter for '{derived_title}'): "
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
        print(f"[ERROR] {msg}", file=sys.stderr)
        sys.exit(1)
    print(f"[WARN] {msg}")
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
            f"{a.name} ({a.stat().st_size / (1024 * 1024):.2f} MB)" for a in assets
        )
        release_kind = "Pre-release (draft/staging on GitHub)" if is_prerelease else "Full Release"

        print("\n================ CONFIRM GITHUB RELEASE ================")
        print(f"Tag           : {tag}")
        print(f"Release Type  : {release_kind}")
        print(f"Release Title : {current_title}")
        print(f"Assets        : {asset_summary}")
        print("--------------------------------------------------------")
        print(current_notes)
        print("========================================================\n")

        choice = (
            input(
                "Create this pre-release on GitHub now? "
                "[y = publish pre-release / e = edit title & notes / n = cancel]: "
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
            print("[INFO] Cancelled before creating GitHub pre-release.")
            sys.exit(0)


def main() -> None:
    parser = argparse.ArgumentParser(
        description="Root-directory release script for Android, KMP, and Flutter."
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
        "--full-release",
        action="store_true",
        help="Optional flag to publish as a full release instead of a pre-release",
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
    remote = cfg.get("remote", "origin")
    is_prerelease = False if args.full_release else bool(cfg.get("prerelease", True))

    # 1. Keystore guard, release notes version read, README version check, and already-published check
    jks_path = find_jks_keystore(cfg)
    guard_keystore_not_tracked(jks_path)
    if jks_path:
        print(f"[OK] Found keystore in root: {jks_path.name}")

    _, version, derived_title, release_notes, parsed_code = load_release_notes(cfg)
    verify_readme_matches_version(cfg, version)

    tag = f"v{version}"
    if is_release_already_published(tag, remote):
        print(
            f"[ERROR] Release '{tag}' is already published on GitHub (or tag already exists on {remote}). "
            "Update your release note & README version first.",
            file=sys.stderr,
        )
        sys.exit(1)

    # 2. Optional runtime prompts (never asks for version)
    version_code = prompt_optional_code(args.code, parsed_code)
    release_title = prompt_optional_title(args.title, derived_title)
    build_ios = prompt_optional_ios_toggle(cfg, args.with_ios, args.skip_ios)
    explicit_apk = prompt_optional_path(args.apk, "APK")
    explicit_ipa = prompt_optional_path(args.ipa, "iOS artifact") if build_ios else None

    # 3. Sync version files, commit & push
    update_version_files(cfg, version, version_code)

    run(["git", "add", "-A"])
    has_changes = (
            subprocess.run(["git", "diff", "--cached", "--quiet"], cwd=ROOT_DIR).returncode
            != 0
    )
    if has_changes:
        run(["git", "commit", "-m", f"chore(release): {tag}"])
        run(["git", "push", remote, "HEAD"])

    # 4. Build Android / KMP APK (interactive console so password prompts work)
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

    # 5. Optionally build iOS artifact
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

    # 6. Explicit confirmation before tagging and publishing on GitHub
    if not args.auto_publish:
        release_title, release_notes = confirm_before_github_release(
            tag, release_title, release_notes, assets, is_prerelease
        )

    # 7. Create & push matching Git tag, then publish GitHub Pre-release with notes & assets
    gh_exe = resolve_gh_executable()
    if not gh_exe:
        print(
            "[ERROR] GitHub CLI ('gh') is required to publish the release. "
            "Install it ('winget install --id GitHub.cli') and run 'gh auth login'.",
            file=sys.stderr,
        )
        sys.exit(1)

    with tempfile.NamedTemporaryFile(
            mode="w", suffix=".md", delete=False, encoding="utf-8"
    ) as notes_tmp:
        notes_tmp.write(release_notes + "\n")
        notes_tmp_path = Path(notes_tmp.name)

    try:
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
    finally:
        if notes_tmp_path.exists():
            notes_tmp_path.unlink(missing_ok=True)

    uploaded_names = ", ".join(a.name for a in assets)
    label_kind = "pre-release" if is_prerelease else "release"
    print(
        f"\n[DONE] Published GitHub {label_kind} {tag} ('{release_title}') with: {uploaded_names}"
    )


if __name__ == "__main__":
    main()