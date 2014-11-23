import conversions.QuasiNotes;
import conversions.Spectrum;
import conversions.TimeSeries;
import conversions.WaveletSpectrumTransform;
import conversions.fourier.BlackmanWindow;
import conversions.fourier.STFT;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;

public class WaveletSpectrumTransformRunner {
    public static void main(String[] args) {
        int timeStepLength = 256;
        int windowLength = 2048;

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

            double[] counts = new double[result.getPowerSpectrum().get(0).length];
            double nu0 = result.getFrequencyZeroPoint();
            double dnu = result.getFrequencyStep();
            for (int j = 0; j < counts.length; ++j) {
                counts[j] = nu0 + j * dnu;
            }

            WaveletSpectrumTransform noteGetterWithCounts = new WaveletSpectrumTransform(result, counts);
            ArrayList<double[]> alignedPower = result.getAlignedPowerSpectrum();

            ArrayList<double[]> subSpectrum2 = noteGetterWithCounts.spectrumTransformWithCounts(alignedPower);

            double t0 = result.getTimeZeroPoint();
            double dt = result.getTimeStep();

            PrintStream outNotes2 = new PrintStream(new File(outputDir, inputFileName + ".wt2point.dat"));

            for (int i = 0; i < subSpectrum2.size(); ++i) {
                double criticalNoise = QuasiNotes.getCriticalNoise(alignedPower.get(i), 0.03);
                for (int j = 0; j < subSpectrum2.get(i).length; j++) {
                    outNotes2.println((i * dt + t0) + "   " + counts[j] + "   " + subSpectrum2.get(i)[j] + "   " + criticalNoise);
                }
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
