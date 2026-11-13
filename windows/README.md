# LyricEcho for Windows

A Windows desktop companion for displaying synchronized lyrics and translations while listening to music.

## Requirements

- Windows 10 version 19041 or later, or Windows 11
- .NET 8 SDK to build, or .NET 8 Desktop Runtime to run a framework-dependent build

## Build and run

```powershell
dotnet restore
dotnet run --project .\LyricEcho.Windows.csproj
```

## Features

- Detect media sessions and display synchronized lyrics from LRCLIB
- Display original lyrics and translations in a floating window
- Translate using Gemini or an OpenAI-compatible service
- Import LRC files, browse locally saved lyrics, and manage favorites
- Browse saved lyrics and translations without interrupting the currently playing track
- Choose a Persian, English, Arabic, Spanish, or Turkish app interface
- Configure appearance, playback integration, and translation profiles

Configure your own API key in the app. Keys and profiles are stored locally using Windows account protection and are not included in this project. API requests may incur charges depending on the chosen provider.

Media detection and controls depend on the information and controls exposed by the active Windows player. The Windows app does not yet include all Android features.

The Android project is available in the [LyricEcho AI repository](https://github.com/shinigami6669/LyricEcho-AI). The original project is distributed under the MIT license.

## Windows release

Download `LyricEcho.Windows.exe` from the release assets and run it on 64-bit Windows 10 version 19041 or later, or Windows 11. The release executable includes the .NET runtime, so a separate .NET installation is not required. Windows may show a warning because the executable is not digitally signed.

When the main window is closed, LyricEcho continues running in the notification area. Double-click its icon to reopen it or use the tray menu to quit. Choose a translation provider and enter your own key in Settings. Automatic translation and the floating window can be switched on or off there.
