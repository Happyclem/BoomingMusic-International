# Keeping this fork in sync with upstream

This fork ("International edition") tracks
[mardous/BoomingMusic](https://github.com/mardous/BoomingMusic) and adds multilingual-lyrics
features on top. Use this procedure to pull upstream changes without losing our work.

## One-time setup

Add the upstream remote (already done on the maintainer's machine):

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
   git log upstream/master --oneline -20   # what's new upstream
   ```

4. **Make a safety branch** so you can always get back:

   ```bash
   git branch -f backup-before-upstream-merge master
   ```

5. **Merge upstream into master:**

   ```bash
   git checkout master
   git merge upstream/master --no-edit
   ```

   We use **merge** (not rebase) so our published history stays stable and doesn't require a
   force-push.

6. **Resolve conflicts.** Files that tend to conflict because we changed them:

   - `app/src/main/res/values/donottranslate.xml` — keep our `app_name`
     (`BoomingMusicInternational`).
   - `README.md` — keep our fork intro / screenshots section; take upstream's Credits/Supporters.
   - `.gitignore` — keep both sides' entries.
   - `app/src/main/res/values/strings.xml` — keep our added strings (lyrics export, translation
     colors, what's-new). Usually auto-merges.

   After editing, `git add <file>` each resolved file, then `git commit --no-edit`.

7. **Verify our features still build and pass:**

   ```bash
   ./gradlew :app:compileNormalDebugKotlin
   ./gradlew :app:testNormalDebugUnitTest --tests "com.mardous.booming.data.local.lyrics.*"
   ```

   Then smoke-test in the app: multilingual lyrics (stacked translations), the language selector,
   per-language translation colors (color wheel), lyrics export, and settings search.

8. **Push:**

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
- Export stored lyrics to an external `.lrc` / `.ttml` file.
- Settings search.
- App name: `BoomingMusicInternational`.
