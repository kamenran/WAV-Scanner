# WAV Scanner

WAV Scanner is a minimal desktop app that analyzes `.wav` and `.mp3` audio files and estimates the musical key. Drop in an audio file, or choose one with the native file picker, and the result appears in a clean music-themed window.

## Features

- Drag-and-drop `.wav` and `.mp3` scanning
- Bundled MP3 decoder support; no `ffmpeg` or `avconv` setup required
- Native file picker on macOS and Windows
- Estimated musical key
- Average tuning offset in cents
- Top detected notes
- Double-click launchers for macOS and Windows
- Release zip packaging for GitHub downloads

## Requirements

- Java 17 or newer to run the app
- JDK 17 or newer to build from source

## Download From GitHub

Download the source from GitHub, then use the launcher for your system:

1. Click `Code`.
2. Click `Download ZIP`.
3. Unzip the repository.
4. Open the project folder.

Then launch the app:

- macOS: double-click `Launch WAV Scanner.command`
- Windows: double-click `Launch WAV Scanner.bat`

The launcher builds `dist/WAV-Scanner.jar` automatically if it does not exist yet.
The Windows launcher is the only `.bat` file regular users need to double-click.

## Build From Source

Clone the repository:

```bash
git clone https://github.com/kamenran/WAV-Scanner.git
cd WAV-Scanner
```

### macOS / Linux

```bash
./build.sh
```

This creates:

- `dist/WAV-Scanner.jar`
- `dist/Launch WAV Scanner.command`
- `dist/Launch WAV Scanner.bat`
- `dist/packages/WAV-Scanner-mac.zip`
- `dist/packages/WAV-Scanner-windows.zip`

On macOS, double-click `dist/Launch WAV Scanner.command`.

If macOS says the script is not executable, run:

```bash
chmod +x build.sh "Launch WAV Scanner.command"
```

### Windows

```bat
scripts\build-windows.bat
```

This creates:

- `dist\WAV-Scanner.jar`
- `dist\Launch WAV Scanner.bat`

Double-click `dist\Launch WAV Scanner.bat`.

## Optional GitHub Release Packaging

If you want to publish prebuilt downloads later, run:

```bash
./build.sh
```

Then attach these generated local files to a GitHub Release:

- `dist/packages/WAV-Scanner-mac.zip`
- `dist/packages/WAV-Scanner-windows.zip`

To build a Windows `.exe` installer instead, run this on Windows with JDK 17+ installed:

```bat
scripts\build-windows-exe.bat
```

`jpackage` may require the WiX Toolset for `.exe` installer output.

## Development Run

macOS / Linux:

```bash
javac -cp "libs/*" -d out src/WAVKeyDetector.java
java -cp "libs/*:out" WAVKeyDetector
```

Windows:

```bat
javac -cp "libs/*" -d out src\WAVKeyDetector.java
java -cp "libs/*;out" WAVKeyDetector
```

## Notes

- Best results come from simple, clear audio such as vocals, single instruments, or isolated notes.
- Dense full mixes and drum-heavy audio may produce less reliable key estimates.
- Build output is ignored by Git. Commit source files and libraries, then attach generated zip files to GitHub Releases.
