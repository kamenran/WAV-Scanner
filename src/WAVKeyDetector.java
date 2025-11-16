import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;
import be.tarsos.dsp.pitch.PitchDetectionHandler;
import be.tarsos.dsp.pitch.PitchDetectionResult;
import be.tarsos.dsp.pitch.PitchProcessor;
import be.tarsos.dsp.util.PitchConverter;

import javax.swing.*;
import java.io.File;
import java.util.*;

public class WAVKeyDetector {

    private static final Map<Integer, Integer> pitchFrequencyMap = new HashMap<>();
    private static final List<Double> centsOffsets = new ArrayList<>();

    public static void main(String[] args) {
        System.out.println("Starting WAV Key Detection...");

        String filePath = promptUserForWavFile();
        if (filePath == null) {
            System.out.println("No valid file selected. Exiting.");
            return;
        }

        analyzeAudioFile(filePath);
        determineMusicalKey();
    }

    // Choose WAV File
    private static String promptUserForWavFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Choose a WAV file");

        int choice = fileChooser.showOpenDialog(null);
        if (choice == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            if (!file.getName().toLowerCase().endsWith(".wav")) {
                System.err.println("Oops! That's not a WAV file.");
                return null;
            }
            return file.getAbsolutePath();
        }
        return null;
    }

    // Pitch information
    private static void analyzeAudioFile(String filePath) {
        try {
            AudioDispatcher dispatcher = AudioDispatcherFactory.fromPipe(filePath, 44100, 1024, 0);
            PitchDetectionHandler handler = (PitchDetectionResult result, AudioEvent event) -> {
                if (result.getPitch() > 0) {
                    storePitchData(result.getPitch());
                }
            };

            dispatcher.addAudioProcessor(new PitchProcessor(
                    PitchProcessor.PitchEstimationAlgorithm.FFT_YIN,
                    44100, 1024, handler
            ));

            System.out.println("Processing audio file...");
            dispatcher.run();
            System.out.println("Finished processing.");
        } catch (Exception e) {
            System.err.println("Error: Couldn't process the audio file. Details: " + e.getMessage());
            e.printStackTrace(); // More human-like debugging
        }
    }

    // Converts to MIDI and stores frequency
    private static void storePitchData(double pitch) {
        int midiNote = (int) Math.round(PitchConverter.hertzToMidiKey(pitch));
        double centsOffset = (PitchConverter.hertzToMidiKey(pitch) - midiNote) * 100;
        if (pitchFrequencyMap.containsKey(midiNote)) {
            pitchFrequencyMap.put(midiNote, pitchFrequencyMap.get(midiNote) + 1);
        } else {
            pitchFrequencyMap.put(midiNote, 1);
        }

        centsOffsets.add(centsOffset);
    }

    // Finds most likely key
    private static void determineMusicalKey() {
        if (pitchFrequencyMap.isEmpty()) {
            System.out.println("No significant pitch data found. Key analysis failed.");
            return;
        }

        List<Integer> topNotes = getMostFrequentNotes(3);
        List<String> noteNames = new ArrayList<>();

        for (int note : topNotes) {
            noteNames.add(convertMidiToNoteName(note));
        }

        String estimatedKey = matchNotesToKey(noteNames);
        double avgCentsOffset = 0.0;
        for (double offset : centsOffsets) {
            avgCentsOffset += offset;
        }
        avgCentsOffset = avgCentsOffset / centsOffsets.size();

        System.out.printf("Estimated Key: %s (Avg tuning offset: %.2f cents)\n", estimatedKey, avgCentsOffset);
    }

    // Converts to note
    private static String convertMidiToNoteName(int midiNote) {
        String[] notes = {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};
        return notes[midiNote % 12];
    }

    // Most frequent notes returned
    private static List<Integer> getMostFrequentNotes(int count) {
        List<Map.Entry<Integer, Integer>> sortedNotes = new ArrayList<>(pitchFrequencyMap.entrySet());
        sortedNotes.sort((a, b) -> b.getValue() - a.getValue());

        List<Integer> result = new ArrayList<>();
        for (int i = 0; i < count && i < sortedNotes.size(); i++) {
            result.add(sortedNotes.get(i).getKey());
        }
        return result;
    }

    // Determines actual key
    private static String matchNotesToKey(List<String> detectedNotes) {
        Map<String, List<String>> keySignatures = new HashMap<>();
        keySignatures.put("C Major", Arrays.asList("C", "D", "E", "F", "G", "A", "B"));
        keySignatures.put("A Minor", Arrays.asList("A", "B", "C", "D", "E", "F", "G"));
        keySignatures.put("G Major", Arrays.asList("G", "A", "B", "C", "D", "E", "F#"));
        keySignatures.put("E Minor", Arrays.asList("E", "F#", "G", "A", "B", "C", "D"));

        String bestMatch = "Unknown";
        int maxMatches = 0;

        for (Map.Entry<String, List<String>> key : keySignatures.entrySet()) {
            int matchCount = 0;
            for (String note : detectedNotes) {
                if (key.getValue().contains(note)) {
                    matchCount++;
                }
            }
            if (matchCount > maxMatches) {
                maxMatches = matchCount;
                bestMatch = key.getKey();
            }
        }

        return bestMatch;
    }
}
