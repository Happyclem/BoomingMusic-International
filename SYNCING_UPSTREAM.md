# Keeping this fork in sync with upstream

This fork ("International edition") tracks
[mardous/BoomingMusic](https://github.com/mardous/BoomingMusic) and adds multilingual-lyrics
features (plus a few playback/interface tweaks) on top. Use this procedure to pull upstream
changes without losing our work.

> 💡 On Windows, run the Git commands in **Git Bash** and the Gradle commands in **PowerShell**
> (see [Build & verify on Windows](#build--verify-on-windows) for the `JAVA_HOME` setup). The
> commands are the same on macOS/Linux, just use `./gradlew` instead of `.\gradlew.bat`.

## ⚡ Version rapide (celle que tu utiliseras le plus souvent)

La mise à jour courante depuis upstream. Colle chaque bloc dans le **terminal PowerShell de
VSCode**. Pour les détails, les cas de conflit et le build de release signé, voir les sections
complètes plus bas.

**1. Mettre à jour l'app** — merge upstream, vérifie que ça compile, puis pousse **seulement si la
compilation réussit** :

```powershell
git checkout master
git fetch upstream
git branch -f backup-before-upstream-merge master
git merge upstream/master --no-edit
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot"
.\gradlew.bat :app:compileGithubDebugKotlin
if ($LASTEXITCODE -eq 0) { git push origin master; Write-Host ">>> OK : fusionne + pousse. Tu n'es plus en retard." } else { Write-Host ">>> STOP : la compilation est cassee. Tape:  git reset --hard backup-before-upstream-merge  puis reviens vers moi." }
```

**2. Générer l'APK** (~10 min, attends `BUILD SUCCESSFUL`) :

```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot"
.\gradlew.bat :app:assembleGithubRelease --console=plain
```

**3. Ouvrir le dossier de l'APK** — prends le fichier `...-github-universal.apk` :

```powershell
explorer app\build\outputs\apk\github\release
```

> 💡 Rappels : un merge « propre » côté Git peut quand même casser la compilation (d'où l'étape de
> vérification), et si tu **publies** l'APK, pense à monter la version dans
> [`app/build.gradle.kts`](app/build.gradle.kts) avant le build 2.

## One-time setup

Add the upstream remote (only needed once per clone):

```bash
git remote add upstream https://github.com/mardous/BoomingMusic.git
```

Check it with `git remote -v`; you should see both `origin` (this fork) and `upstream`.

## Every time you sync

1. **Make sure your working tree is clean** (`git status`). Commit or stash anything pending.

2. **Fetch upstream:**

   ```bash
   git fetch upstream --tags
   ```

3. **See how far behind/ahead you are:**

   ```bash
   # left = commits only upstream has, right = commits only we have
   git rev-list --left-right --count upstream/master...master
   git log --oneline --no-merges master..upstream/master   # what's new upstream
   ```

   If the left number is `0`, you're already up to date — stop here.

4. **Preview the conflicts before touching anything** (optional but recommended):

   ```bash
   git merge-tree --write-tree --name-only master upstream/master
   ```

   The file names it prints after the first line are the ones that will actually conflict.
   Everything else auto-merges.

5. **Make a safety branch** so you can always get back:

   ```bash
   git branch -f backup-before-upstream-merge master
   ```

6. **Merge upstream into master:**

   ```bash
   git checkout master
   git merge upstream/master --no-edit
   ```

   We use **merge** (not rebase) so our published history stays stable and doesn't require a
   force-push.

7. **Resolve conflicts.** Files that tend to conflict because we changed them:

   - `README.md` — **keep our fork structure** (the "Multilingual lyrics (this fork)" and
     "Playback & interface improvements" sections). If upstream adds a table-of-contents or new
     top sections, **drop them** — they link to sections our README doesn't have.
   - `app/src/main/res/values/donottranslate.xml` — keep our `app_name`
     (`BoomingMusicInternational`).
   - `.gitignore` — keep both sides' entries.
   - `app/src/main/res/values/strings.xml` — keep our added strings (lyrics export, translation
     colors, what's-new). Usually auto-merges.

   After editing each file, run `git add <file>`, then finish with `git commit --no-edit`.

8. **Watch out for _semantic_ overlaps (no conflict markers, but duplicated work).**
   Git can auto-merge two changes that do the *same thing* two different ways. Upstream sometimes
   ships a feature we already added. Known examples so far:

   - **"Play all songs when searching"** — upstream added a *Settings toggle*
     (`play_all_songs_when_searching`); our fork added a *"Play all songs from here"* menu item in
     search results (#386). Both can coexist; just be aware they overlap.
   - **Long-track total-time fix (#433)** — upstream set `songTotalTime` width to `wrap_content`;
     we kept `minWidth="40dp"`. They combine fine.

   These don't break the build, but review them so the app doesn't grow redundant UI.

9. **Verify our features still build and pass** (see the Windows setup below for `JAVA_HOME`):

   ```powershell
   .\gradlew.bat :app:compileGithubDebugKotlin
   .\gradlew.bat :app:testGithubDebugUnitTest --tests "com.mardous.booming.data.local.lyrics.*"
   ```

   > ⚠️ **Flavor renamed.** Upstream renamed the `normal` product flavor to **`github`**
   > (July 2026). All task names changed accordingly: `assembleNormalDebug` →
   > `assembleGithubDebug`, `compileNormalDebugKotlin` → `compileGithubDebugKotlin`, etc.

   Then smoke-test in the app: multilingual lyrics (stacked translations), the language selector,
   per-language translation colors (color wheel), lyrics export, settings search, the floating
   notification, and the media-notification close button.

10. **Push:**

    ```bash
    git push origin master
    ```

    A plain `push` is enough because we merged (no history rewrite). If you ever rebased instead,
    you'd need `git push --force-with-lease`.

## If a merge goes wrong

```bash
git merge --abort              # during an unfinished merge
# or, to fully reset to the pre-merge state:
git reset --hard backup-before-upstream-merge
```

## Our fork-specific features (don't let these regress)

- Multilingual lyrics: stacked original + multiple translations (TTML / eLRC).
- Translation language selector (globe menu).
- Language badge next to translated lines.
- Per-language translation colors with an HSV color-wheel picker.
- Export stored lyrics to an external `.lrc` / `.ttml` file; link a lyrics file per song.
- Settings search.
- Floating media notification + notification "close" button.
- Previous / Next album cover-gesture actions.
- Reading song metadata via TagLib instead of Android MediaStore (ID3v2.4 fix).
- App name: `BoomingMusicInternational`.

---

## Build & verify on Windows

The JDK isn't on `PATH`, so set `JAVA_HOME` in **PowerShell** before every Gradle session:

```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot"
$env:GRADLE_OPTS = "-Dorg.gradle.daemon=false"
```

- Android SDK is at `%LOCALAPPDATA%\Android\Sdk` (recorded in `local.properties`, which is
  git-ignored — don't commit it).
- The first cold KSP/Room compile is slow (~30 min); later builds are much faster.

## Build a signed release APK (a "real", non-debug build)

1. **One-time: create a release keystore.** Android requires you to sign every future update with
   the *same* key, so keep this file safe and back it up. From the repo root, in Git Bash:

   ```bash
   "$JAVA_HOME/bin/keytool.exe" -genkeypair -v \
     -keystore booming-release.jks -alias booming \
     -keyalg RSA -keysize 2048 -validity 10000 \
     -storepass "YOUR_PASSWORD" -keypass "YOUR_PASSWORD" \
     -dname "CN=Booming Music International, O=Happyclem, C=FR"
   ```

2. **One-time: create `keystore.properties`** in the repo root (it is git-ignored — never commit
   it, nor the `.jks`):

   ```properties
   keyAlias=booming
   keyPassword=YOUR_PASSWORD
   storePassword=YOUR_PASSWORD
   storeFile=C:/Users/ticle/Documents/Git/BoomingMusic-International/booming-release.jks
   ```

   Use forward slashes in `storeFile` (a `\` is an escape character in `.properties` files).
   If `keystore.properties` is missing, the release build silently falls back to the **debug** key,
   which is *not* a real release signature.

3. **Set the version** in [`app/build.gradle.kts`](app/build.gradle.kts): edit `currentVersion`
   (e.g. `Version.Stable(versionMajor = 1, versionMinor = 6, versionPatch = 0)`) **and** the
   matching `versionCode` in `defaultConfig`. The build has a `check(versionCode == currentVersionCode)`
   that fails fast if the two don't agree, so run
   `.\gradlew.bat :app:signingReport` first to confirm the config is valid and that `githubRelease`
   points at your keystore (not the debug one).

4. **Build:**

   ```powershell
   .\gradlew.bat :app:assembleGithubRelease --console=plain
   ```

5. **Collect the APKs** from `app/build/outputs/apk/github/release/`. Because ABI splits are on,
   you'll get one per architecture plus a universal one, e.g.:

   - `BoomingMusic-International-1.6.0-github-arm64-v8a.apk` ← install this on most phones
   - `…-armeabi-v7a.apk`, `…-x86_64.apk`, `…-x86.apk`
   - `…-github-universal.apk` ← works on any device (larger)

   Install with `adb install -r <apk>` or by copying it to the phone.
