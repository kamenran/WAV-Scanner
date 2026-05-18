import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.io.jvm.JVMAudioInputStream;
import be.tarsos.dsp.pitch.PitchDetectionHandler;
import be.tarsos.dsp.pitch.PitchDetectionResult;
import be.tarsos.dsp.pitch.PitchProcessor;
import be.tarsos.dsp.util.PitchConverter;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDropEvent;
import java.io.File;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class WAVKeyDetector {

    private static final int BUFFER_SIZE = 1024;
    private static final int BUFFER_OVERLAP = 0;
    private static final Color BACKGROUND = new Color(246, 244, 239);
    private static final Color CARD = Color.WHITE;
    private static final Color INK = new Color(24, 28, 34);
    private static final Color MUTED = new Color(111, 115, 118);
    private static final Color ACCENT = new Color(24, 126, 117);
    private static final Color ACCENT_DARK = new Color(16, 91, 86);
    private static final Color WARM = new Color(197, 120, 68);
    private static final Color BORDER = new Color(225, 219, 209);
    private static final Color SOFT_LINE = new Color(236, 229, 218);

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            configureLookAndFeel();
            ScannerWindow window = new ScannerWindow();
            window.setVisible(true);

            if (args.length > 0) {
                window.analyzeFile(new File(args[0]));
            }
        });
    }

    private static void configureLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            try {
                UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
            } catch (Exception ignoredAgain) {
                // Swing's default is acceptable if the platform look and feel is unavailable.
            }
        }
    }

    private static AnalysisResult analyzeAudioFile(File file) throws Exception {
        Map<Integer, Integer> pitchFrequencyMap = new HashMap<>();
        List<Double> centsOffsets = new ArrayList<>();

        DispatcherSource source = createDispatcher(file);
        AudioDispatcher dispatcher = source.dispatcher();
        PitchDetectionHandler handler = (PitchDetectionResult result, AudioEvent event) -> {
            if (result.getPitch() > 0) {
                storePitchData(result.getPitch(), pitchFrequencyMap, centsOffsets);
            }
        };

        dispatcher.addAudioProcessor(new PitchProcessor(
                PitchProcessor.PitchEstimationAlgorithm.FFT_YIN,
                source.sampleRate(),
                BUFFER_SIZE,
                handler
        ));

        try {
            dispatcher.run();
        } catch (RuntimeException exception) {
            throw exception;
        }

        if (pitchFrequencyMap.isEmpty() || centsOffsets.isEmpty()) {
            throw new IllegalStateException("No clear pitch data was found in this file.");
        }

        List<Integer> topMidiNotes = getMostFrequentNotes(pitchFrequencyMap, 5);
        List<String> topNotes = new ArrayList<>();

        for (int note : topMidiNotes) {
            String noteName = convertMidiToNoteName(note);
            if (!topNotes.contains(noteName)) {
                topNotes.add(noteName);
            }
        }

        double avgCentsOffset = 0.0;
        for (double offset : centsOffsets) {
            avgCentsOffset += offset;
        }
        avgCentsOffset = avgCentsOffset / centsOffsets.size();

        return new AnalysisResult(
                matchNotesToKey(topNotes),
                avgCentsOffset,
                topNotes,
                centsOffsets.size()
        );
    }

    private static DispatcherSource createDispatcher(File file) throws Exception {
        AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(file);
        AudioFormat sourceFormat = audioInputStream.getFormat();
        AudioFormat decodedFormat = new AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                sourceFormat.getSampleRate(),
                16,
                sourceFormat.getChannels(),
                sourceFormat.getChannels() * 2,
                sourceFormat.getSampleRate(),
                false
        );

        AudioInputStream decodedStream = AudioSystem.getAudioInputStream(decodedFormat, audioInputStream);
        AudioDispatcher dispatcher = new AudioDispatcher(
                new JVMAudioInputStream(decodedStream),
                BUFFER_SIZE,
                BUFFER_OVERLAP
        );

        return new DispatcherSource(dispatcher, Math.round(decodedFormat.getSampleRate()));
    }

    private static boolean isSupportedAudioFile(File file) {
        String name = file.getName().toLowerCase();
        return name.endsWith(".wav") || name.endsWith(".mp3");
    }

    private static void storePitchData(
            double pitch,
            Map<Integer, Integer> pitchFrequencyMap,
            List<Double> centsOffsets
    ) {
        int midiNote = (int) Math.round(PitchConverter.hertzToMidiKey(pitch));
        double centsOffset = (PitchConverter.hertzToMidiKey(pitch) - midiNote) * 100;
        pitchFrequencyMap.put(midiNote, pitchFrequencyMap.getOrDefault(midiNote, 0) + 1);
        centsOffsets.add(centsOffset);
    }

    private static String convertMidiToNoteName(int midiNote) {
        String[] notes = {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};
        return notes[Math.floorMod(midiNote, 12)];
    }

    private static List<Integer> getMostFrequentNotes(Map<Integer, Integer> pitchFrequencyMap, int count) {
        List<Map.Entry<Integer, Integer>> sortedNotes = new ArrayList<>(pitchFrequencyMap.entrySet());
        sortedNotes.sort((a, b) -> b.getValue() - a.getValue());

        List<Integer> result = new ArrayList<>();
        for (int i = 0; i < count && i < sortedNotes.size(); i++) {
            result.add(sortedNotes.get(i).getKey());
        }
        return result;
    }

    private static String matchNotesToKey(List<String> detectedNotes) {
        Map<String, List<String>> keySignatures = new LinkedHashMap<>();
        keySignatures.put("C Major", Arrays.asList("C", "D", "E", "F", "G", "A", "B"));
        keySignatures.put("G Major", Arrays.asList("G", "A", "B", "C", "D", "E", "F#"));
        keySignatures.put("D Major", Arrays.asList("D", "E", "F#", "G", "A", "B", "C#"));
        keySignatures.put("A Major", Arrays.asList("A", "B", "C#", "D", "E", "F#", "G#"));
        keySignatures.put("E Major", Arrays.asList("E", "F#", "G#", "A", "B", "C#", "D#"));
        keySignatures.put("B Major", Arrays.asList("B", "C#", "D#", "E", "F#", "G#", "A#"));
        keySignatures.put("F# Major", Arrays.asList("F#", "G#", "A#", "B", "C#", "D#", "F"));
        keySignatures.put("C# Major", Arrays.asList("C#", "D#", "F", "F#", "G#", "A#", "C"));
        keySignatures.put("F Major", Arrays.asList("F", "G", "A", "A#", "C", "D", "E"));
        keySignatures.put("A# Major", Arrays.asList("A#", "C", "D", "D#", "F", "G", "A"));
        keySignatures.put("D# Major", Arrays.asList("D#", "F", "G", "G#", "A#", "C", "D"));
        keySignatures.put("G# Major", Arrays.asList("G#", "A#", "C", "C#", "D#", "F", "G"));
        keySignatures.put("A Minor", Arrays.asList("A", "B", "C", "D", "E", "F", "G"));
        keySignatures.put("E Minor", Arrays.asList("E", "F#", "G", "A", "B", "C", "D"));
        keySignatures.put("B Minor", Arrays.asList("B", "C#", "D", "E", "F#", "G", "A"));
        keySignatures.put("F# Minor", Arrays.asList("F#", "G#", "A", "B", "C#", "D", "E"));
        keySignatures.put("C# Minor", Arrays.asList("C#", "D#", "E", "F#", "G#", "A", "B"));
        keySignatures.put("G# Minor", Arrays.asList("G#", "A#", "B", "C#", "D#", "E", "F#"));
        keySignatures.put("D# Minor", Arrays.asList("D#", "F", "F#", "G#", "A#", "B", "C#"));
        keySignatures.put("A# Minor", Arrays.asList("A#", "C", "C#", "D#", "F", "F#", "G#"));
        keySignatures.put("D Minor", Arrays.asList("D", "E", "F", "G", "A", "A#", "C"));
        keySignatures.put("G Minor", Arrays.asList("G", "A", "A#", "C", "D", "D#", "F"));
        keySignatures.put("C Minor", Arrays.asList("C", "D", "D#", "F", "G", "G#", "A#"));
        keySignatures.put("F Minor", Arrays.asList("F", "G", "G#", "A#", "C", "C#", "D#"));

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

    private static class ScannerWindow extends JFrame {
        private final JLabel titleLabel = new JLabel("WAV Scanner", SwingConstants.CENTER);
        private final JLabel subtitleLabel = new JLabel("drop a .wav or .mp3 file to find its musical key", SwingConstants.CENTER);
        private final JLabel fileLabel = new JLabel("No file selected", SwingConstants.CENTER);
        private final JLabel keyLabel = new JLabel("--");
        private final JLabel detailLabel = new JLabel("Waiting for audio", SwingConstants.CENTER);
        private final JLabel notesLabel = new JLabel("notes appear here", SwingConstants.CENTER);
        private final JLabel tuningLabel = new JLabel("tuning --", SwingConstants.CENTER);
        private final JProgressBar progressBar = new JProgressBar();
        private final JButton chooseButton = new JButton("Choose Audio File");
        private DropPanel dropPanel;

        ScannerWindow() {
            super("WAV Scanner");
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            setMinimumSize(new Dimension(680, 520));
            setLocationByPlatform(true);

            JPanel root = new StagePanel();
            root.setLayout(new GridBagLayout());
            root.setBorder(BorderFactory.createEmptyBorder(34, 34, 34, 34));
            setContentPane(root);

            JPanel card = buildCard();
            root.add(card, centeredConstraints());

            chooseButton.addActionListener(event -> chooseFile());
            installDropTarget(card);

            pack();
            setSize(760, 560);
        }

        void analyzeFile(File file) {
            if (file == null) {
                return;
            }

            if (!isSupportedAudioFile(file)) {
                showError("Please choose a .wav or .mp3 file.");
                return;
            }

            chooseButton.setEnabled(false);
            progressBar.setVisible(true);
            progressBar.setIndeterminate(true);
            fileLabel.setText(file.getName());
            keyLabel.setText("Scanning");
            detailLabel.setText("listening for pitch and tuning");
            notesLabel.setText("analyzing waveform");
            tuningLabel.setText("tuning --");
            dropPanel.setActive(true);

            SwingWorker<AnalysisResult, Void> worker = new SwingWorker<>() {
                @Override
                protected AnalysisResult doInBackground() throws Exception {
                    return analyzeAudioFile(file);
                }

                @Override
                protected void done() {
                    chooseButton.setEnabled(true);
                    progressBar.setVisible(false);
                    progressBar.setIndeterminate(false);
                    dropPanel.setActive(false);

                    try {
                        showResult(get());
                    } catch (Exception exception) {
                        showError(exception.getCause() == null
                                ? exception.getMessage()
                                : exception.getCause().getMessage());
                    }
                }
            };

            worker.execute();
        }

        private JPanel buildCard() {
            JPanel card = new JPanel(new GridBagLayout());
            card.setBackground(CARD);
            card.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(BORDER),
                    BorderFactory.createEmptyBorder(34, 38, 34, 38)
            ));

            titleLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 36));
            titleLabel.setForeground(INK);

            subtitleLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 15));
            subtitleLabel.setForeground(MUTED);

            fileLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
            fileLabel.setForeground(MUTED);

            keyLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 58));
            keyLabel.setForeground(ACCENT_DARK);
            keyLabel.setHorizontalAlignment(SwingConstants.CENTER);

            detailLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
            detailLabel.setForeground(MUTED);

            notesLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
            notesLabel.setForeground(INK);

            tuningLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
            tuningLabel.setForeground(MUTED);

            progressBar.setVisible(false);
            progressBar.setPreferredSize(new Dimension(280, 5));
            progressBar.setBorderPainted(false);
            progressBar.setForeground(ACCENT);
            progressBar.setBackground(SOFT_LINE);

            styleButton(chooseButton);
            dropPanel = new DropPanel(keyLabel, detailLabel);
            dropPanel.setPreferredSize(new Dimension(520, 190));

            addRow(card, titleLabel, 0, 0, 1.0, new Insets(0, 0, 8, 0));
            addRow(card, subtitleLabel, 1, 0, 1.0, new Insets(0, 0, 26, 0));
            addRow(card, dropPanel, 2, 0, 1.0, new Insets(0, 0, 18, 0));
            addRow(card, progressBar, 3, 0, 1.0, new Insets(0, 0, 18, 0));
            addRow(card, notesLabel, 4, 0, 1.0, new Insets(0, 0, 7, 0));
            addRow(card, tuningLabel, 5, 0, 1.0, new Insets(0, 0, 18, 0));
            addRow(card, fileLabel, 6, 0, 1.0, new Insets(0, 0, 16, 0));
            addRow(card, chooseButton, 7, 0, 1.0, new Insets(0, 0, 0, 0));

            return card;
        }

        private void chooseFile() {
            FileDialog fileDialog = new FileDialog(this, "Choose an audio file", FileDialog.LOAD);
            fileDialog.setFilenameFilter((directory, name) -> {
                String lowerName = name.toLowerCase();
                return lowerName.endsWith(".wav") || lowerName.endsWith(".mp3");
            });
            fileDialog.setVisible(true);

            String fileName = fileDialog.getFile();
            String directory = fileDialog.getDirectory();
            if (fileName != null && directory != null) {
                analyzeFile(new File(directory, fileName));
            }
        }

        private void showResult(AnalysisResult result) {
            keyLabel.setText(result.key());
            detailLabel.setText("estimated key");
            notesLabel.setText("top notes  " + String.join("  /  ", result.topNotes()));
            tuningLabel.setText(String.format(
                    "average tuning %.2f cents  |  %,d pitch samples",
                    result.averageCentsOffset(),
                    result.sampleCount()
            ));
        }

        private void showError(String message) {
            keyLabel.setText("Try again");
            detailLabel.setText(message == null || message.isBlank()
                    ? "That file could not be analyzed."
                    : message);
            notesLabel.setText("notes appear here");
            tuningLabel.setText("tuning --");
            progressBar.setVisible(false);
            chooseButton.setEnabled(true);
            if (dropPanel != null) {
                dropPanel.setActive(false);
            }
        }

        private void installDropTarget(Component component) {
            new DropTarget(component, new DropTargetAdapter() {
                @Override
                public void drop(DropTargetDropEvent event) {
                    try {
                        event.acceptDrop(event.getDropAction());
                        List<?> droppedFiles = (List<?>) event.getTransferable()
                                .getTransferData(DataFlavor.javaFileListFlavor);

                        if (!droppedFiles.isEmpty() && droppedFiles.get(0) instanceof File file) {
                            analyzeFile(file);
                        }
                    } catch (Exception exception) {
                        showError("That drop could not be read.");
                    }
                }
            });
        }

        private static GridBagConstraints centeredConstraints() {
            GridBagConstraints constraints = new GridBagConstraints();
            constraints.gridx = 0;
            constraints.gridy = 0;
            constraints.weightx = 1.0;
            constraints.weighty = 1.0;
            constraints.fill = GridBagConstraints.BOTH;
            return constraints;
        }

        private static void styleButton(JButton button) {
            button.setBackground(ACCENT);
            button.setForeground(Color.WHITE);
            button.setFocusPainted(false);
            button.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(ACCENT_DARK),
                    BorderFactory.createEmptyBorder(12, 24, 12, 24)
            ));
            button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            button.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        }
    }

    private static void addRow(
            JPanel panel,
            JComponent component,
            int row,
            int fill,
            double weightX,
            Insets insets
    ) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = row;
        constraints.fill = fill == 0 ? GridBagConstraints.HORIZONTAL : fill;
        constraints.weightx = weightX;
        constraints.insets = insets;
        panel.add(component, constraints);
    }

    private static class StagePanel extends JPanel {
        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setPaint(new GradientPaint(0, 0, BACKGROUND, getWidth(), getHeight(), new Color(237, 244, 241)));
            g.fillRect(0, 0, getWidth(), getHeight());

            g.setColor(new Color(225, 220, 209, 110));
            g.setStroke(new BasicStroke(1f));
            int centerY = getHeight() / 2;
            for (int i = -2; i <= 2; i++) {
                g.drawLine(40, centerY + i * 14, getWidth() - 40, centerY + i * 14);
            }

            g.setColor(new Color(24, 126, 117, 38));
            g.setStroke(new BasicStroke(2f));
            int baseY = centerY + 4;
            int previousX = 40;
            int previousY = baseY;
            for (int x = 41; x < getWidth() - 40; x += 10) {
                double wave = Math.sin(x * 0.035) * 20 + Math.sin(x * 0.012) * 11;
                int y = baseY + (int) wave;
                g.drawLine(previousX, previousY, x, y);
                previousX = x;
                previousY = y;
            }
            g.dispose();
        }
    }

    private static class DropPanel extends JPanel {
        private final JLabel keyLabel;
        private final JLabel detailLabel;
        private boolean active;

        DropPanel(JLabel keyLabel, JLabel detailLabel) {
            super(new GridBagLayout());
            this.keyLabel = keyLabel;
            this.detailLabel = detailLabel;
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(24, 30, 24, 30));

            addRow(this, keyLabel, 0, 0, 1.0, new Insets(18, 0, 4, 0));
            addRow(this, detailLabel, 1, 0, 1.0, new Insets(0, 0, 16, 0));
        }

        void setActive(boolean active) {
            this.active = active;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int width = getWidth();
            int height = getHeight();
            Color fill = active ? new Color(232, 246, 243) : new Color(250, 248, 244);
            g.setColor(fill);
            g.fillRoundRect(0, 0, width - 1, height - 1, 18, 18);

            g.setColor(active ? ACCENT : BORDER);
            g.setStroke(new BasicStroke(active ? 2.4f : 1.4f));
            g.drawRoundRect(0, 0, width - 1, height - 1, 18, 18);

            paintWaveform(g, width, height);
            paintBadge(g, width);
            g.dispose();
        }

        private void paintWaveform(Graphics2D g, int width, int height) {
            g.setColor(new Color(197, 120, 68, active ? 95 : 55));
            g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int centerY = height / 2 + 26;
            int lastX = 34;
            int lastY = centerY;
            for (int x = 35; x < width - 34; x += 9) {
                double wave = Math.sin(x * 0.05) * 12 + Math.sin(x * 0.12) * 5;
                int y = centerY + (int) wave;
                g.drawLine(lastX, lastY, x, y);
                lastX = x;
                lastY = y;
            }
        }

        private void paintBadge(Graphics2D g, int width) {
            String badge = "WAV / MP3";
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
            FontMetrics metrics = g.getFontMetrics();
            int badgeWidth = metrics.stringWidth(badge) + 24;
            int x = (width - badgeWidth) / 2;
            g.setColor(active ? ACCENT : INK);
            g.fillRoundRect(x, 20, badgeWidth, 28, 14, 14);
            g.setColor(Color.WHITE);
            g.drawString(badge, x + 12, 39);
        }
    }

    private record AnalysisResult(
            String key,
            double averageCentsOffset,
            List<String> topNotes,
            int sampleCount
    ) {
    }

    private record DispatcherSource(
            AudioDispatcher dispatcher,
            int sampleRate
    ) {
    }
}
