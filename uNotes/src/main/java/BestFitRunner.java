import conversions.QuasiNotes;
import conversions.Spectrum;
import conversions.TimeSeries;
import conversions.fourier.BlackmanWindow;
import conversions.fourier.STFT;
import conversions.notes.Note;
import conversions.notes.NoteSequence;
import conversions.notes.NoteSequenceComparator;

import javax.sound.midi.MidiSystem;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.File;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Calendar;
import java.util.StringJoiner;

public class BestFitRunner {

    public static void main(String[] args) {
        double timeStart = System.nanoTime();
        try {
            File inputDir = new File("test", "music");
            File outputDir = new File("test", "output");
            outputDir.mkdir();

            System.out.println("uNotes");
            System.out.println(java.time.LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_TIME));
            //for (String inputFileName : Arrays.asList("tarantella_mono_guitar", "tarantella_2guitar")) {
            for (String inputFileName : Arrays.asList("a")) {
                String inputFileNameWav = inputFileName + ".wav";
                File in = new File(inputDir, inputFileNameWav);
                System.out.println(inputFileNameWav);

                PrintStream outDistances = new PrintStream(new File(outputDir, inputFileNameWav + ".dist"));
                PrintStream outBestFit = new PrintStream(new File(outputDir, inputFileNameWav + ".bestfit"));
                File outBestFitMidi = new File(outputDir, inputFileNameWav + ".bestfit.mid");

                File inputMidi = new File(inputDir, inputFileName + ".mid");
                NoteSequence inputNoteSequence = new NoteSequence(inputMidi);
                int tempoInBPM = inputNoteSequence.getTempoInBPM();
                //int ticksPerQuarterNote = 3 * 4;   // Time resolution up to usual 16th notes and/or 32th trioles
                int ticksPerQuarterNote = 4;    //  Time resolution up to 16th notes

                FitParams bestFit = new FitParams(0, 0, 0, 0, 0, Double.MAX_VALUE);
                bestFit.distance = Double.MAX_VALUE;
                NoteSequence bestNoteSequence = new NoteSequence(inputMidi);    // Initialize to avoid compiler error

                for (int timeStepLength = 256 / 2; timeStepLength <= 256 * 2; timeStepLength *= 2) {
                    System.out.println("timeStepLength = " + timeStepLength);
                    for (int windowLength = 4096 / 2; windowLength <= 4096 * 2; windowLength *= 2) {
                        System.out.println("\t windowLength = " + windowLength);
                        AudioInputStream stream = AudioSystem.getAudioInputStream(in);
                        TimeSeries series = new TimeSeries(stream);
                        series.start();
                        STFT stft = new STFT(windowLength, timeStepLength, new BlackmanWindow());
                        Spectrum spectrum = stft.transform(series);

                        System.out.println("\t stft completed");

                        for (double relativePowerThreshold = 5;
                             relativePowerThreshold <= 25; relativePowerThreshold += 5) {
                            for (double powerThreshold = 0; powerThreshold <= 5; powerThreshold += 5) {
                                for (double statisticalSignificance = 0.001;
                                     statisticalSignificance < 0.01; statisticalSignificance += 0.001) {

                                    QuasiNotes quasiNotes = new QuasiNotes(spectrum, Note.C.midiCode(1), Note.B.midiCode(6),
                                            relativePowerThreshold, powerThreshold, statisticalSignificance);
                                    NoteSequence noteSequence = new NoteSequence(quasiNotes, tempoInBPM, ticksPerQuarterNote);
                                    int length = (int) inputNoteSequence.getTicksLength();

                                    double distance = NoteSequenceComparator.distance(inputNoteSequence, noteSequence, length);

                                    FitParams fitParams = new FitParams(timeStepLength, windowLength,
                                            relativePowerThreshold, powerThreshold, statisticalSignificance,
                                            distance);
                                    outDistances.println(fitParams);
                                    System.out.println("\t\t" + fitParams);

                                    if (distance < bestFit.distance) {
                                        bestFit = fitParams;
                                        bestNoteSequence = noteSequence;
                                    }
                                }
                            }
                        }
                    }
                }
                outBestFit.println(bestFit);
                MidiSystem.write(bestNoteSequence.getMidiSequence(), 0, outBestFitMidi);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            double timeEnd = System.nanoTime();
            System.out.println("Total running time = " + (timeEnd - timeStart) / 1e9 + " seconds");
        }
    }
}

class FitParams {
    public Integer timeStepLength;
    public Integer windowLength;
    public Double relativePowerThreshold;
    public Double powerThreshold;
    public Double statisticalSignificance;
    public Double distance;

    FitParams(int timeStepLength, int windowLength,
              double relativePowerThreshold, double powerThreshold, double statisticalSignificance,
              double distance) {
        this.timeStepLength = timeStepLength;
        this.windowLength = windowLength;
        this.relativePowerThreshold = relativePowerThreshold;
        this.powerThreshold = powerThreshold;
        this.statisticalSignificance = statisticalSignificance;
        this.distance = distance;
    }

    @Override
    public String toString() {
        StringJoiner s = new StringJoiner(" ");
        s.add(timeStepLength.toString());
        s.add(windowLength.toString());
        s.add(relativePowerThreshold.toString());
        s.add(powerThreshold.toString());
        s.add(statisticalSignificance.toString());
        s.add(distance.toString());
        return s.toString();
    }
}