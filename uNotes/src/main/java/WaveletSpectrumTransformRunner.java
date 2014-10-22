import conversions.Spectrum;
import conversions.TimeSeries;
import conversions.WaveletSpectrumTransform;
import conversions.fourier.BlackmanWindow;
import conversions.fourier.STFT;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.File;
import java.io.PrintStream;
import java.util.Vector;

/**
 * Created by User on 21.10.2014.
 */
public class WaveletSpectrumTransformRunner {
    public static void main(String[] args) {
        int timeStepLength = 200;
        int windowLength = 4096;

        File dir = new File("test", "music");
        String inputFileName = "Am_chords.wav";
        File in = new File(dir, inputFileName);

        System.out.println("uNotes");

        try {
            AudioInputStream stream = AudioSystem.getAudioInputStream(in);

            TimeSeries series = new TimeSeries(stream);
            series.start();

            STFT stft = new STFT(windowLength, timeStepLength, new BlackmanWindow());
            Spectrum result = stft.transform(series);

            WaveletSpectrumTransform noteGetter = new WaveletSpectrumTransform(result);


            Vector<double[]> power = result.getPowerSpectrum();

            double t0 = result.getTimeZeroPoint();
            double nu0 = result.getFrequencyZeroPoint();

            double dt = result.getTimeStep();
            double dnu = result.getFrequencyStep();
            PrintStream out = new PrintStream(new File(inputFileName + ".power.dat"));

            for (int i = 0; i < power.size(); ++i) {
                for (int j = 0; j < power.elementAt(i).length; j++) {
                    out.println(i * dt + t0 + "   " + j * dnu + nu0 + "  " + power.elementAt(i)[j]);
                }
            }

            PrintStream outNotes = new PrintStream(new File(inputFileName + ".wt2.dat"));

            dnu = result.getFrequencyStep() / noteGetter.ALPHA;
            nu0 = 2 * dnu;

            for (int i = 0; i < power.size(); ++i) {
                double[] slice = noteGetter.transform(i);
                for (int j = 0; j < slice.length; j++) {
                    outNotes.println((dt * i + t0) + "   " + (nu0 + dnu * j) + "  " + slice[j]);
                }
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
