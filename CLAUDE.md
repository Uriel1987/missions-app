# MissionsApp — notes for Claude

## Releasing to the phone (GitHub Releases + Obtainium) — the ONLY way to ship builds

The app is installed on the user's phone(s) via **Obtainium**, tracking GitHub Releases of
this repo (`Uriel1987/missions-app`). Do **not** hand the user debug APKs or ask them to
uninstall/reinstall — debug builds are signed with a different key and won't install as updates.

- Commit the change, then run `.\release.ps1 -Notes "what changed"` from the repo root. It builds a
  signed release APK, pushes, and runs `gh release create` with the APK attached. The user then taps
  Update in Obtainium.
- Versions are automatic: `versionCode` = git commit count, tag/`versionName` = `1.0.<count>`
  (passed as `-PappVersionCode` / `-PappVersionName`). Never hardcode them in `build.gradle.kts`.
- Signing uses the shared keystore `C:\Users\Riley\keys\release.jks`; passwords are only in
  `C:\Users\Riley\.gradle\gradle.properties` (`RELEASE_*`). Never commit keystores or passwords,
  never generate a new keystore — a different key means the app can't be updated in place.
- `applicationId` (`com.ourhouse.missions`) must never change, or it becomes a different app.
- `app/google-services.json` is git-ignored; if missing, re-download it from the Firebase console.
- Env: JAVA_HOME = `C:\Program Files\Android\Android Studio\jbr`, ANDROID_HOME =
  `%LOCALAPPDATA%\Android\Sdk` (the script sets both if unset).
