import com.sun.istack.internal.NotNull;
import conversions.QuasiNotes;
import conversions.Spectrum;
import conversions.TimeSeries;
import conversions.fourier.BlackmanWindow;
import conversions.fourier.STFT;
import conversions.notes.Note;
import conversions.notes.NoteSequence;

import javax.sound.midi.MidiSystem;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;

public class NoteExtractorRunner {
    public static void main(String[] args) {
        double timeStart = System.nanoTime();
        int timeStepLength = 256;
        int windowLength = 4096;

        File inputDir = new File("test", "music");
        File outputDir = new File("test", "output");
        outputDir.mkdir();

        String inputFileName = "Rondo alla Turka.wav";
        File in = new File(inputDir, inputFileName);

        System.out.println("uNotes");

        try {
            AudioInputStream stream = AudioSystem.getAudioInputStream(in);

            TimeSeries series = new TimeSeries(stream);
            series.start();

            STFT stft = new STFT(windowLength, timeStepLength, new BlackmanWindow());
            Spectrum spectrum = stft.transform(series);

            ArrayList<double[]> power = spectrum.getPowerSpectrum();

            double t0 = spectrum.getTimeZeroPoint();
            double nu0 = spectrum.getFrequencyZeroPoint();

            double dt = spectrum.getTimeStep();
            double dnu = spectrum.getFrequencyStep();
            /*
            PrintStream out = new PrintStream(new File(outputDir, inputFileName + ".power.dat"));
            for (int i = 0; i < power.size(); ++i) {
                for (int j = 0; j < power.get(i).length; j++) {
                    out.println((i * dt + t0) + "   " + (j * dnu + nu0) + "  " + power.get(i)[j]);
                }
            }
            */

            //
            double relativePowerThreshold = 20;
            double powerThreshold = 0;
            double statisticalSignificance = 0.003;
            double absolutePowerThreshold = 0;

            PrintStream outNotes = new PrintStream(new File(outputDir, inputFileName + ".npw.dat"));

            QuasiNotes quasiNotes = new QuasiNotes(series, windowLength / 2, timeStepLength,
                    spectrum, Note.C.midiCode(1), Note.B.midiCode(6), relativePowerThreshold,
                    powerThreshold, statisticalSignificance, absolutePowerThreshold);
            ArrayList<double[]> notePower = quasiNotes.getNotePowerSeries();
            for (int i = 0; i < notePower.size(); ++i) {
                outNotes.print(i * dt + " ");
                for (int j = 0; j < notePower.get(i).length; j++) {
                    outNotes.print(notePower.get(i)[j] + " ");
                }
                outNotes.println();
            }
            //

            //
            //MIDI output
            File outMidi = new File(outputDir, inputFileName + ".npw.mid");
            NoteSequence noteSequence = new NoteSequence(quasiNotes);
            MidiSystem.write(noteSequence.getMidiSequence(), 0, outMidi);
            //
            ArrayList<Double> alignedAmplitude = series.getAlignedAmplitude(windowLength / 2, timeStepLength);
            PrintStream outAmplitude = new PrintStream(new File(outputDir, inputFileName + ".amp.dat"));
            double[] hist = histogram(alignedAmplitude, 20);
            /*
            for (int i = 0; i < alignedAmplitude.size(); ++i) {
                outAmplitude.println((1.0 * (i * timeStepLength + windowLength / 2) / series.getSampleRate()) + "   " + alignedAmplitude.get(i));
            }
            */
            //
            for (int i = 0; i < hist.length; ++i) {
                outAmplitude.println(i + "  " + hist[i]);
            }
            //
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            double timeEnd = System.nanoTime();
            System.out.println("Total running time = " + (timeEnd - timeStart) / 1e9 + " seconds");
        }
    }

    public static double[] histogram(@NotNull ArrayList<Double> value, int n) {
        if (n < 1) {
            throw new IllegalArgumentException("n must be positive");
        }
        double[] answer = new double[n];
        double min = value.get(0);
        double max = value.get(0);
        for (int i = 1; i < value.size(); ++i) {
            if (value.get(i) < min) {
                min = value.get(i);
            }
            if (value.get(i) > max) {
                max = value.get(i);
            }
        }
        double delta = (max - min) / n;
        for (int i = 0; i < value.size(); ++i) {
            boolean flag = true;
            int j = 0;
            while ((j < n) && (flag)) {
                if ((value.get(i) > (min - 1.0e-9 + j * delta)) && (value.get(i) < (min + 1.0e-9 + (j + 1) * delta))) {
                    answer[j] += 1.0;
                    flag = false;
                }
                ++j;
            }
        }
        return answer;
    }
}
