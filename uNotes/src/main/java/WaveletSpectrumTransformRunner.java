import conversions.*;
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
        int timeStepLength = 256;
        int windowLength = 2048;

        File dir = new File("test", "music");
        String inputFileName = "a.wav";
        File in = new File(dir, inputFileName);

        System.out.println("uNotes");

        try {
            AudioInputStream stream = AudioSystem.getAudioInputStream(in);

            TimeSeries series = new TimeSeries(stream);
            series.start();

            STFT stft = new STFT(windowLength, timeStepLength, new BlackmanWindow());
            Spectrum result = stft.transform(series);

            Vector<double[]> power = result.getPowerSpectrum();

            double t0 = result.getTimeZeroPoint();
            double nu0 = result.getFrequencyZeroPoint();

            double dt = result.getTimeStep();
            double dnu = result.getFrequencyStep();
            PrintStream out = new PrintStream(new File(inputFileName + ".power.dat"));

            //
            //result.alignment(20);
            //

            for (int i = 0; i < power.size(); ++i) {
                for (int j = 0; j < power.elementAt(i).length; j++) {
                    out.println((i * dt + t0) + "   " + (j * dnu + nu0) + "  " + power.elementAt(i)[j]);
                }
            }

            //
            result.alignment(20);
            //

            WaveletSpectrumTransform noteGetter = new WaveletSpectrumTransform(result);

            Spectrum subSpectrum = noteGetter.spectrumTransform(result);

            power = subSpectrum.getPowerSpectrum();

            t0 = subSpectrum.getTimeZeroPoint();
            nu0 = subSpectrum.getFrequencyZeroPoint();

            dt = subSpectrum.getTimeStep();
            dnu = subSpectrum.getFrequencyStep();
            PrintStream outNotes = new PrintStream(new File(inputFileName + ".wt2.dat"));

            for (int i = 0; i < power.size(); ++i) {
                for (int j = 1; j < power.elementAt(i).length; j++) {
                    outNotes.println((i * dt + t0) + "   " + (j * dnu + nu0) + "   " + power.elementAt(i)[j]);
                }
            }

            ///////////////////////////////////////////////////////

            int spectrumLength = result.getPowerSpectrum().elementAt(0).length;

            double counts[] = new double[spectrumLength - 2];
            for (int i = 0; i < spectrumLength - 2; ++i) {
                counts[i] = (i + 2) * result.getFrequencyStep() / WaveletSpectrumTransform.ALPHA;
            }

            WaveletSpectrumTransform noteGetterWithCounts = new WaveletSpectrumTransform(result, counts);

            Spectrum subSpectrum2 = noteGetter.spectrumTransform(result);

            power = subSpectrum2.getPowerSpectrum();

            t0 = subSpectrum2.getTimeZeroPoint();
            nu0 = subSpectrum2.getFrequencyZeroPoint();

            dt = subSpectrum2.getTimeStep();
            dnu = subSpectrum2.getFrequencyStep();
            PrintStream outNotes2 = new PrintStream(new File(inputFileName + ".wt2point.dat"));

            for (int i = 0; i < power.size(); ++i) {
                for (int j = 1; j < power.elementAt(i).length; j++) {
                    outNotes2.println((i * dt + t0) + "   " + (j * dnu + nu0) + "   " + power.elementAt(i)[j]);
                }
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
