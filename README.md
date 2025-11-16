# WAV Scanner (Console-Run Version)

WAV Scanner is a lightweight Java application that analyzes `.wav` audio files and estimates their musical key. It uses the TarsosDSP digital signal processing library to perform pitch detection, extract note information, and compute tuning offsets. This version is designed to run directly through the console using standard JDK tools.

## Features
- Console-run application
- GUI `.wav` file chooser (Swing)
- Real-time pitch detection with TarsosDSP
- Note aggregation and filtering
- Major/minor key signature matching
- Average tuning offset calculation

## Project Structure
WAV-Scanner-PlainJava/
├── src/
│   └── WAVKeyDetector.java
├── libs/
│   ├── TarsosDSP-2.4.1.jar
│   └── core-2.5.jar
└── README.md

## Prerequisites
- Java 17 or later
- JDK tools (`javac`, `java`) available on PATH
- The `libs` directory must remain in the same folder as `src/`

## Running the Program Through the Console

### Step 1 — Navigate into the project folder
cd WAV-Scanner-PlainJava

### Step 2 — Compile the program
macOS / Linux:
javac -cp "libs/*" src/WAVKeyDetector.java

Windows:
javac -cp "libs/*" src\WAVKeyDetector.java

### Step 3 — Run the program
macOS / Linux:
java -cp "libs/*:src" WAVKeyDetector

Windows:
java -cp "libs/*;src" WAVKeyDetector

A file chooser window will appear. Select a `.wav` file to begin analysis. Results will print directly in the terminal.

## How the Program Works
1. Prompts the user to select a `.wav` file.
2. Streams audio through the TarsosDSP AudioDispatcher.
3. Performs frame-by-frame pitch detection.
4. Converts detected frequencies to MIDI notes and note names.
5. Aggregates pitch data across the entire file.
6. Matches detected notes to major/minor key signatures.
7. Outputs the estimated key and tuning offset to the console.

## Notes
- The algorithm is designed for simple, monophonic audio (vocals, single instruments, isolated notes).
- Full beats, drums, or multilayered mixes may produce limited or no pitch data due to overlapping harmonics.

## Technology Stack
- Java 17
- TarsosDSP
- Swing (JFileChooser)
