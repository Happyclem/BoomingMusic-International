# Sample multilingual lyrics (F1 manual testing)

These files exercise **Feature 1 — multiple stacked translations**. They are **not** part of
the app build; they are throwaway fixtures for testing on a device. All text is generic
placeholder text, **not** real lyrics.

## How to use

1. Pick any short audio file on your device, e.g. `Song.mp3`.
2. Copy one of the sample files next to it, renamed to the **same basename** so Booming picks it
   up as a sidecar:
   - `Song.lrc`  ← for the ELRC samples (keep the `.lrc` extension)
   - `Song.ttml` ← for the TTML samples
3. Open the song, show lyrics, and play. The active line should show the original on top with
   every translation stacked underneath.

> Only one sidecar (`.lrc` **or** `.ttml`) is read per song, so test them one at a time.

## What to look for

| File | Expectation |
|------|-------------|
| `elrc_multi_translation.lrc` | Word-by-word highlight on the original; **two** translation lines (fr, es) stacked below, no highlight. |
| `ttml_multi_translation.ttml` | Same as above but via TTML `x-translation` spans. The translation matching your phone's language (if fr/es) is shown first. |
| `control_no_translation.lrc` | Original only — must look exactly like before this feature. |
| `control_one_translation.lrc` | Original + one translation — must look exactly like before this feature. |

## Local unit tests (run these on your machine, not on device)

The parser logic is covered by JVM unit tests:

```
./gradlew :app:testNormalDebugUnitTest --tests "com.mardous.booming.data.local.lyrics.*"
```

> These need the Android SDK configured (Android Studio sets `local.properties` automatically).
> The TTML test relies on a real `XmlPullParser` implementation (`kxml2`, added as a
> `testImplementation`); if `XmlPullParserFactory.newInstance()` ever fails to resolve in the
> JVM test runtime, that is the place to look.
