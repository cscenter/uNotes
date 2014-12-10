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
            String inputFileName = "Rondo alla Turka";
            processFile(inputDir, outputDir, inputFileName);
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

        for (int timeStepLength = 256 / 1; timeStepLength <= 256 * 1; timeStepLength *= 2) {
            System.out.println("timeStepLength = " + timeStepLength);
            for (int windowLength = 4096 / 1; windowLength <= 4096 * 1; windowLength *= 2) {
                System.out.println("\t windowLength = " + windowLength);

                FourierFitParams fourierFitParams = new FourierFitParams(timeStepLength, windowLength);

                AudioInputStream stream = AudioSystem.getAudioInputStream(in);
                TimeSeries series = new TimeSeries(stream);
                series.start();
                STFT stft = new STFT(fourierFitParams.windowLength, fourierFitParams.timeStepLength, new BlackmanWindow());
                Spectrum spectrum = stft.transform(series);

                System.out.println("\t stft completed");

                for (double relativePowerThreshold = 10;
                     relativePowerThreshold <= 20; relativePowerThreshold += 5) {
                    for (double powerThreshold = 0; powerThreshold <= 5; powerThreshold += 5) {
                        for (double absolutePowerThreshold = -20;
                             absolutePowerThreshold <= 0; absolutePowerThreshold += 10) {
                            for (double statisticalSignificance = 0.01;
                                 statisticalSignificance < 0.05; statisticalSignificance += 0.01) {
                                for (double firstOvertoneFactor = 0.4;
                                     firstOvertoneFactor <= 0.6; firstOvertoneFactor += 0.1) {
                                    QuasiNotesFitParams quasiNotesFitParams = new QuasiNotesFitParams(relativePowerThreshold,
                                            powerThreshold, absolutePowerThreshold, statisticalSignificance, firstOvertoneFactor);

                                    QuasiNotes quasiNotes = new QuasiNotes(series, windowLength / 2, timeStepLength,
                                            spectrum, Note.C.midiCode(1), Note.B.midiCode(6), relativePowerThreshold,
                                            powerThreshold, statisticalSignificance, absolutePowerThreshold, firstOvertoneFactor);
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
    public Double relativePowerThreshold = 20.0;
    public Double powerThreshold = 0.0;
    public Double statisticalSignificance = 0.01;
    public Double absolutePowerThreshold = -20.0;
    public Double firstOvertoneFactor = 0.5;

    QuasiNotesFitParams() {
    }

    QuasiNotesFitParams(double relativePowerThreshold, double powerThreshold, double absolutePowerThreshold,
                        double statisticalSignificance, double firstOvertoneFactor) {
        this.relativePowerThreshold = relativePowerThreshold;
        this.powerThreshold = powerThreshold;
        this.absolutePowerThreshold = absolutePowerThreshold;
        this.statisticalSignificance = statisticalSignificance;
        this.firstOvertoneFactor = firstOvertoneFactor;
    }

    @Override
    public String toString() {
        StringJoiner s = new StringJoiner(" ");
        s.add(relativePowerThreshold.toString());
        s.add(powerThreshold.toString());
        s.add(absolutePowerThreshold.toString());
        s.add(statisticalSignificance.toString());
        s.add(firstOvertoneFactor.toString());
        return s.toString();
    }
}