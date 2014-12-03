import conversions.Spectrum;
import conversions.TimeSeries;
import conversions.fourier.BlackmanWindow;
import conversions.fourier.STFT;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;

public class FourierSpectrumTransformRunner {
    public static void main(String[] args) {
        double timeStart = System.nanoTime();

        int timeStepLength = 10000;
        int windowLength = 512;

        File inputDir = new File("test", "music");
        File outputDir = new File("test", "output");
        outputDir.mkdir();
        String inputFileName = "a.wav";
        File in = new File(inputDir, inputFileName);

        System.out.println("uNotes");

        try {
            AudioInputStream stream = AudioSystem.getAudioInputStream(in);

            TimeSeries series = new TimeSeries(stream);
            series.start();

            STFT stft = new STFT(windowLength, timeStepLength, new BlackmanWindow());
            Spectrum result = stft.transform(series);

            ArrayList<double[]> power = result.getPowerSpectrum();

            double t0 = result.getTimeZeroPoint();
            double nu0 = result.getFrequencyZeroPoint();

            double dt = result.getTimeStep();
            double dnu = result.getFrequencyStep();
            PrintStream out = new PrintStream(new File(outputDir, inputFileName + ".power.dat"));

            for (int i = 0; i < power.size(); ++i) {
                for (int j = 0; j < power.get(i).length; j++) {
                    out.println((i * dt + t0) + "   " + (j * dnu + nu0) + "  " + power.get(i)[j]);
                }
            }

            STFT noteGetter = new STFT(result, new BlackmanWindow());

            Spectrum subSpectrum = noteGetter.spectrumTransform(result);

            power = subSpectrum.getPowerSpectrum();

            t0 = subSpectrum.getTimeZeroPoint();
            nu0 = subSpectrum.getFrequencyZeroPoint();

            dt = subSpectrum.getTimeStep();
            dnu = subSpectrum.getFrequencyStep();
            PrintStream outNotes = new PrintStream(new File(outputDir, inputFileName + ".ft2.dat"));

            for (int i = 0; i < power.size(); ++i) {
                for (int j = 1; j < power.get(i).length; j++) {
                    outNotes.println((i * dt + t0) + "   " + (1.0 / (j * dnu + nu0)) + "  " + power.get(i)[j]);
                }
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            double timeEnd = System.nanoTime();
            System.out.println("Total running time = " + (timeEnd - timeStart) / 1e9 + " seconds");
        }
    }
}
