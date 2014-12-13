import conversions.QuasiNotes;
import conversions.Spectrum;
import conversions.TimeSeries;
import conversions.fourier.BlackmanWindow;
import conversions.fourier.STFT;
import conversions.notes.Note;
import conversions.notes.NoteSequence;
import conversions.notes.NoteSequenceComparator;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiSystem;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
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
            for (String inputFileName :
                    Arrays.asList("Rondo alla Turka part1")) {
                processFile(inputDir, outputDir, inputFileName);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            double timeEnd = System.nanoTime();
            System.out.println("Total running time = " + (timeEnd - timeStart) / 1e9 + " seconds");
        }
    }

    // TODO rename
    private static void processFile(File inputDir, File outputDir, String inputFileName)
            throws InvalidMidiDataException, IOException, UnsupportedAudioFileException {
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

        FitParams bestFit = new FitParams(new FourierFitParams(), new QuasiNotesFitParams(), Double.MAX_VALUE);
        NoteSequence bestNoteSequence = new NoteSequence(inputMidi);    // Initialize to avoid compiler error

        for (int timeStepLength : Arrays.asList(256)) {
            System.out.println("timeStepLength = " + timeStepLength);
            for (int windowLength : Arrays.asList(4096)) {
                System.out.println("\t windowLength = " + windowLength);

                FourierFitParams fourierFitParams = new FourierFitParams(timeStepLength, windowLength);

                AudioInputStream stream = AudioSystem.getAudioInputStream(in);
                TimeSeries series = new TimeSeries(stream);
                series.start();
                STFT stft = new STFT(fourierFitParams.windowLength, fourierFitParams.timeStepLength, new BlackmanWindow());
                Spectrum spectrum = stft.transform(series);

                System.out.println("\t stft completed");

                for (double relativePowerThreshold : Arrays.asList(15.0)) {
                    for (double powerThreshold : Arrays.asList(5.0)) {
                        for (double absolutePowerThreshold : Arrays.asList(0.0)) {
                            for (double statisticalSignificance : Arrays.asList(0.0001, 0.001, 0.01, 0.05)) {
                                for (double minDuration : Arrays.asList(60.0 / tempoInBPM / 4.0, 60.0 / tempoInBPM / 5.0, 60.0 / tempoInBPM / 6.0)) {
                                    for (double gravy = 0.04; gravy <= 0.2; gravy += 0.04) {
                                        for (double divider = 1.5; divider <= 3.0; divider += 0.7) {
                                            for (double dividerPower = 0.5; dividerPower <= 0.9; dividerPower += 0.2) {
                                                QuasiNotesFitParams quasiNotesFitParams = new QuasiNotesFitParams(relativePowerThreshold,
                                                        powerThreshold, absolutePowerThreshold, statisticalSignificance,
                                                        minDuration, gravy, divider, dividerPower);

                                                QuasiNotes quasiNotes = new QuasiNotes(series, windowLength / 2, timeStepLength,
                                                        spectrum, Note.C.midiCode(1), Note.B.midiCode(6),
                                                        relativePowerThreshold, powerThreshold,
                                                        absolutePowerThreshold, minDuration, gravy, divider, dividerPower);
                                                NoteSequence noteSequence = new NoteSequence(quasiNotes, tempoInBPM, ticksPerQuarterNote);
                                                int length = (int) inputNoteSequence.getTicksLength();

                                                double distance = NoteSequenceComparator.calculateError(noteSequence, inputNoteSequence,
                                                        length, 1.0, 10.0);
                                                FitParams fitParams = new FitParams(fourierFitParams, quasiNotesFitParams, distance);
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
                        }
                    }
                }
            }
        }
        outBestFit.println(bestFit);
        MidiSystem.write(bestNoteSequence.getMidiSequence(), 0, outBestFitMidi);
    }
}

class FitParams {
    public FourierFitParams fourierFitParams;
    public QuasiNotesFitParams quasiNotesFitParams;
    public Double distance;

    FitParams(FourierFitParams fourier, QuasiNotesFitParams quasiNotes, double distance) {
        this.fourierFitParams = fourier;
        this.quasiNotesFitParams = quasiNotes;
        this.distance = distance;
    }

    @Override
    public String toString() {
        return fourierFitParams + " " + quasiNotesFitParams + " " + distance;
    }
}

class FourierFitParams {
    public Integer timeStepLength = 256;
    public Integer windowLength = 4096;

    FourierFitParams() {
    }

    FourierFitParams(int timeStepLength, int windowLength) {
        this.timeStepLength = timeStepLength;
        this.windowLength = windowLength;
    }

    @Override
    public String toString() {
        return timeStepLength + " " + windowLength;
    }
}

class QuasiNotesFitParams {
    public Double relativePowerThreshold = 15.0;
    public Double powerThreshold = 5.0;
    public Double statisticalSignificance = 0.01;
    public Double absolutePowerThreshold = 0.0;
    public Double minDuration = 60.0 / 140.0 / 4;
    public Double gravy = 0.06;
    public Double divider = 2.5;
    public Double dividerPower = 0.75;

    QuasiNotesFitParams() {
    }

    QuasiNotesFitParams(double relativePowerThreshold, double powerThreshold, double absolutePowerThreshold,
                        double statisticalSignificance,
                        double minDuration, double gravy, double divider, double dividerPower) {
        this.relativePowerThreshold = relativePowerThreshold;
        this.powerThreshold = powerThreshold;
        this.absolutePowerThreshold = absolutePowerThreshold;
        this.statisticalSignificance = statisticalSignificance;
        this.minDuration = minDuration;
        this.gravy = gravy;
        this.divider = divider;
        this.dividerPower = dividerPower;
    }

    @Override
    public String toString() {
        StringJoiner s = new StringJoiner(" ");
        s.add(relativePowerThreshold.toString());
        s.add(powerThreshold.toString());
        s.add(absolutePowerThreshold.toString());
        s.add(statisticalSignificance.toString());
        s.add(minDuration.toString());
        s.add(gravy.toString());
        s.add(divider.toString());
        s.add(dividerPower.toString());
        return s.toString();
    }
}