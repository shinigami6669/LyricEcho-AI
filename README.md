<p align="center">
  <img src="assets/banner.svg" alt="LyricEcho AI" width="100%">
</p>

<h1 align="center">Your music, in your words.</h1>
<p align="center">Synchronized lyrics and line-by-line translations for Android and Windows.</p>
<p align="center">
  <a href="https://github.com/shinigami6669/LyricEcho-AI/releases/download/v1.0.1/LyricEcho-AI-v1.0.1-universal.apk"><img src="https://img.shields.io/badge/Android-Download%20APK-45D6A3?style=for-the-badge&amp;logo=android&amp;logoColor=white" alt="Download Android APK"></a>
  <a href="https://github.com/shinigami6669/LyricEcho-AI/releases/download/v1.0.1/LyricEcho.Windows.exe"><img src="https://img.shields.io/badge/Windows-Download%20EXE-8B5CF6?style=for-the-badge&amp;logo=windows&amp;logoColor=white" alt="Download Windows EXE"></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/License-MIT-242B3B?style=for-the-badge" alt="MIT License"></a>
</p>

---

## One song. Two languages.

Follow the original lyrics while reading a translation in your chosen language. LyricEcho detects supported media players, looks up lyrics through LRCLIB, and keeps the current line in view. Its floating window stays close to your music, with playback and timing controls at hand.

<table>
<tr><td width="50%" valign="top">

### Android

A mobile companion for supported music players, with a floating overlay, local library, and translation settings.

**Requires:** Android 7.0 or later.

[Download the Android APK](https://github.com/shinigami6669/LyricEcho-AI/releases/download/v1.0.1/LyricEcho-AI-v1.0.1-universal.apk)

</td><td width="50%" valign="top">

### Windows

A desktop companion with a movable lyrics window, local library, media-session controls, and notification-area support.

**Requires:** 64-bit Windows 10 version 19041 or later, or Windows 11. The single-file release includes the .NET runtime.

[Download the Windows EXE](https://github.com/shinigami6669/LyricEcho-AI/releases/download/v1.0.1/LyricEcho.Windows.exe)

</td></tr>
</table>

## Made for listening

- **Lyrics in sync** — follow timed lyrics and adjust the timing when needed.
- **Translations** — choose a language and connect Gemini or a compatible AI provider using your own credentials.
- **Floating lyrics** — keep the current line visible while using another app.
- **Your library** — return to locally saved songs and translations.
- **Flexible playback** — detect supported players and use the available media controls.

Features and player support can differ between Android and Windows. Translation requires an available provider and may incur usage charges. Lyrics and timing depend on the available source.

## Get started

1. Download the file for your platform from the links above.
2. Install the Android APK or run the Windows executable.
3. In settings, select a translation provider, enter your own API key, and choose a target language.
4. Play a track in a supported music player, or search for a song manually.

On Android, enable the requested media notification and overlay permissions for automatic detection and the floating window. On Windows, closing the main window keeps LyricEcho running in the notification area. Use its tray icon to reopen or exit the app. The Windows executable is currently unsigned, so Windows may display a publisher warning.

## Build from source

| Platform | Project | Requirements | Command |
| --- | --- | --- | --- |
| Android | Repository root | JDK 17 and Android SDK | `./gradlew assembleDebug` |
| Windows | [`windows/`](windows/) | .NET 8 SDK and Windows | `dotnet run --project windows/LyricEcho.Windows.csproj` |

For Android, optionally configure a Gemini key locally using `.env.example` as a template, or enter it in the app. The Windows project includes a [build and configuration guide](windows/README.md). Never commit credentials or local application data.

## فارسی

**LyricEcho** متن آهنگ و ترجمه خط‌به‌خط آن را در اندروید و ویندوز نمایش می‌دهد. پنجره شناور، کتابخانه محلی و تنظیم زمان به شما کمک می‌کنند هنگام گوش دادن، متن را دنبال کنید. زبان و سرویس ترجمه را در تنظیمات انتخاب کنید و کلید API خودتان را وارد کنید.

[دانلود اندروید](https://github.com/shinigami6669/LyricEcho-AI/releases/download/v1.0.1/LyricEcho-AI-v1.0.1-universal.apk) · [دانلود ویندوز](https://github.com/shinigami6669/LyricEcho-AI/releases/download/v1.0.1/LyricEcho.Windows.exe)

## License

LyricEcho AI is distributed under the [MIT License](LICENSE).
